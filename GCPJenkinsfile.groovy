pipeline {
    agent any

    environment {
        IMAGE_NAME = "automation"
        BUILD_TAG = "${IMAGE_NAME}:${BUILD_NUMBER}"

        DOCKERHUB_REPO = "balamaheshward/${IMAGE_NAME}"
    }

    stages {

        /* 1. Checkout Code */
        stage('Checkout') {
            steps {
                git branch: 'main', 
                    credentialsId: 'Github2', 
                    url: 'https://github.com/DBalamaheshwar/Devopsautomation.git'
            }
        }

        /* 2. Build Docker Image */
        stage('Docker Build') {
            steps {
                powershell """
                    Write-Host '[INFO] Fixing Docker builder...'
                    docker buildx rm default 2>$null
                    docker buildx create --use --driver=docker

                    Write-Host '[INFO] Building Docker Image...'
                    docker build -t ${IMAGE_NAME}:latest .
                """
            }
        }

        /* 3. Push to Docker Hub */
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    powershell """
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

        /* 4. Security Scans */
        stage('Security Scans (Parallel)') {
            parallel {

                /* Trivy Scan */
                stage('Trivy Scan') {
                    steps {
                        powershell """
                            Write-Host '[INFO] Running Trivy Scan...'
                            
                            New-Item -ItemType Directory -Force -Path reports | Out-Null

                            docker run --rm `
                                -v //var/run/docker.sock:/var/run/docker.sock `
                                aquasec/trivy image --format json `
                                -o reports/trivy-report.json `
                                ${IMAGE_NAME}:latest || exit 0
                        """
                        archiveArtifacts artifacts: 'reports/trivy-report.json'
                    }
                }

                /* Syft + Grype Scan */
                stage('Syft + Grype Scan') {
                    steps {
                        powershell """
                            New-Item -ItemType Directory -Force -Path reports | Out-Null

                            Write-Host '[INFO] Running Syft...'
                            docker run --rm `
                                -v ${env:WORKSPACE}:/scan `
                                anchore/syft:latest ${IMAGE_NAME}:latest -o json > reports/syft-sbom.json

                            Write-Host '[INFO] Running Grype...'
                            docker run --rm `
                                -v ${env:WORKSPACE}:/scan `
                                anchore/grype:latest reports/syft-sbom.json -o json > reports/grype-report.json
                        """
                        archiveArtifacts artifacts: 'reports/*.json'
                    }
                }

            }
        }

    }
}
