pipeline {
    agent any

    environment {
        IMAGE_NAME = "bala"
        BUILD_TAG = "${IMAGE_NAME}:${BUILD_NUMBER}"

        // Docker Hub
        DOCKERHUB_REPO = "your-dockerhub-username/${IMAGE_NAME}"

        // GCP
        PROJECT_ID   = "bala-gcp"
        REGION       = "us-central1"
        CLUSTER_NAME = "bala-cluster"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', 
                    credentialsId: 'Github2', 
                    url: 'https://github.com/DBalamaheshwar/Devopsautomation.git'
            }
        }

        stage('Docker Build') {
            steps {
                powershell  """
                    docker build -t ${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    powershell  """

                        Write-Host '[INFO] Logging into Docker Hub...'
                        docker login -u $env:DOCKER_USER -p $env:DOCKER_PASS

                        Write-Host '[INFO] Tagging Image...'
                        docker tag ${IMAGE_NAME}:latest ${DOCKERHUB_REPO}:${BUILD_NUMBER}

                        Write-Host '[INFO] Pushing Image...'
                        docker push ${DOCKERHUB_REPO}:${BUILD_NUMBER}                        
                    """
                }
            }
        }

        stage('Security Scans (Parallel)') {
            parallel {

                stage('Trivy Scan') {
                    steps {
                        powershell  """
                            Write-Host '[INFO] Running Trivy Scan...'
                            mkdir -p  reports -Force | Out-Null
                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy image --format json \
                                -o reports/trivy-report.json \
                                ${IMAGE_NAME}:latest || true
                        """
                        archiveArtifacts artifacts: 'reports/trivy-report.json'
                    }
                }

                stage('Clair Scan') {
                    steps {
                        powershell  """
                            mkdir -p reports
                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                arminc/clair-scanner:latest \
                                -r reports/clair-report.json \
                                -c http://clair:6060 ${IMAGE_NAME}:latest || true
                        """
                        archiveArtifacts artifacts: 'reports/clair-report.json'
                    }
                }

                stage('Syft + Grype Scan') {
                    steps {
                        powershell  """
                            mkdir -p reports
                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                anchore/syft:latest ${IMAGE_NAME}:latest -o json > reports/syft-sbom.json || true

                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                anchore/grype:latest reports/syft-sbom.json -o json > reports/grype-report.json || true
                        """
                        archiveArtifacts artifacts: 'reports/grype-report.json'
                    }
                }

            }
        }

      /*  stage('Create GKE Cluster') {
            steps {
                withCredentials([file(credentialsId: 'gcp-sa', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    powershell  """
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud config set project ${PROJECT_ID}

                        gcloud container clusters create ${CLUSTER_NAME} \
                            --region ${REGION} \
                            --num-nodes=1 || true
                    """
                }
            }
        } */
 /* 
        stage('Deploy to Kubernetes') {
            steps {
                powershell  """
                    gcloud container clusters get-credentials ${CLUSTER_NAME} --region ${REGION}
                    kubectl apply -f k8s/
                """
            }
        }

        stage('Collect Logs & Export') {
            steps {
                powershell  """
                    mkdir -p logs
                    kubectl get pods -A > logs/pods.log
                    kubectl get deploy -A > logs/deployments.log
                    kubectl get events -A > logs/events.log

                    zip -r logs.zip logs/
                """
                archiveArtifacts artifacts: 'logs.zip'
            }
        } */

         /* stage('Cleanup GKE Cluster') {
            steps {
                powershell  """
                    gcloud container clusters delete ${CLUSTER_NAME} \
                        --region ${REGION} --quiet || true
                """
            }
        }*/
    }
}
