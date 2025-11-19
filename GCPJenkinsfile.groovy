pipeline {
    agent any

    environment {
        IMAGE_NAME = "bala"
        BUILD_TAG = "${IMAGE_NAME}:${BUILD_NUMBER}"

        // JFrog
        JFROG_DOCKER_REPO = "bala.jfrog.io/artifactory/docker-local"
        JFROG_XRAY_URL    = "https://bala.jfrog.io/xray"

        // Tenable
        TEN_REGISTRY = "registry.tenable.io/bala"
        TEN_API      = "https://cloud.tenable.com"

        // GCP
        PROJECT_ID   = "bala-gcp"
        REGION       = "us-central1"
        CLUSTER_NAME = "bala-cluster"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Push to JFrog') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jfrog-creds',
                    usernameVariable: 'JF_USER',
                    passwordVariable: 'JF_PASS'
                )]) {
                    sh """
                        echo "$JF_PASS" | docker login ${JFROG_DOCKER_REPO} -u "$JF_USER" --password-stdin
                        docker tag ${IMAGE_NAME}:latest ${JFROG_DOCKER_REPO}/${BUILD_TAG}
                        docker push ${JFROG_DOCKER_REPO}/${BUILD_TAG}
                    """
                }
            }
        }

        stage('Security Scans (Parallel)') {
            parallel {

                stage('Trivy Scan') {
                    steps {
                        sh """
                            mkdir -p reports
                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy image --format json \
                                -o reports/trivy-report.json \
                                ${IMAGE_NAME}:latest || true
                        """
                        archiveArtifacts artifacts: 'reports/trivy-report.json'
                    }
                }

                stage('JFrog Xray Scan') {
                    steps {
                        sh """
                            mkdir -p reports
                            echo "Trigger Xray Scan..."

                            # Create scan request
                            cat > xray-request.json <<EOF
                            {
                              "scan_type": "repository",
                              "parameters": {
                                "repo_path": "docker-local/${IMAGE_NAME}"
                              }
                            }
                            EOF
                        """

                        withCredentials([string(credentialsId: 'jfrog-api', variable: 'JF_API')]) {
                            sh """
                                curl -H "Content-Type: application/json" \
                                     -H "X-JFrog-Art-Api: $JF_API" \
                                     -X POST ${JFROG_XRAY_URL}/api/v1/scan \
                                     -d @xray-request.json \
                                     -o xray-scan-response.json || true

                                cp xray-scan-response.json reports/xray-report.json
                            """
                        }

                        archiveArtifacts artifacts: 'reports/xray-report.json'
                    }
                }

                stage('Tenable Scan') {
                    steps {
                        withCredentials([string(credentialsId: 'tenable-api', variable: 'TEN_KEY')]) {
                            sh """
                                mkdir -p reports

                                curl -s -H "X-ApiKeys: accessKey=$TEN_KEY;secretKey=$TEN_KEY" \
                                     ${TEN_API}/container-security/api/v1/images \
                                     -o reports/tenable-report.json || true
                            """
                        }
                        archiveArtifacts artifacts: 'reports/tenable-report.json'
                    }
                }
            }
        }

        stage('Create GKE Cluster') {
            steps {
                withCredentials([file(credentialsId: 'gcp-sa', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh """
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud config set project ${PROJECT_ID}

                        gcloud container clusters create ${CLUSTER_NAME} \
                            --region ${REGION} \
                            --num-nodes=1 || true
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    gcloud container clusters get-credentials ${CLUSTER_NAME} --region ${REGION}
                    kubectl apply -f k8s/
                """
            }
        }

        stage('Collect Logs & Export') {
            steps {
                sh """
                    mkdir -p logs
                    kubectl get pods -A > logs/pods.log
                    kubectl get deploy -A > logs/deployments.log
                    kubectl get events -A > logs/events.log

                    zip -r logs.zip logs/
                """
                archiveArtifacts artifacts: 'logs.zip'
            }
        }

        stage('Cleanup GKE Cluster') {
            steps {
                sh """
                    gcloud container clusters delete ${CLUSTER_NAME} \
                        --region ${REGION} --quiet || true
                """
            }
        }
    }
}
