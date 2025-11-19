pipeline {
    agent any

    environment {
        DOCKER_REPO = "your_dockerhub/python-webapp"
        IMAGE_TAG = "v${env.BUILD_NUMBER}"
    }

    stages {
      
        stage('Clone Code') {
            steps {
                git 'https://github.com/yourrepo/python-webapp.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t $DOCKER_REPO:$IMAGE_TAG ."
            }
        }

        stage('Trivy Scan') {
            steps {
                sh """
                trivy image --exit-code 0 --format table -o trivy-report.txt $DOCKER_REPO:$IMAGE_TAG
                """
                archiveArtifacts artifacts: 'trivy-report.txt'
            }
        }

        stage('JFrog Xray Scan') {
            steps {
                sh """
                jfrog rt scan --watches=security_watch --format=json $DOCKER_REPO:$IMAGE_TAG > xray-report.json
                """
                archiveArtifacts artifacts: 'xray-report.json'
            }
        }

        stage('Nessus/Tenable Scan') {
            steps {
                sh """
                nessuscli scan --target $DOCKER_REPO:$IMAGE_TAG --output nessus-report.xml
                """
                archiveArtifacts artifacts: 'nessus-report.xml'
            }
        }

        stage('Push Docker Image') {
            steps {
                sh "docker push $DOCKER_REPO:$IMAGE_TAG"
            }
        }

        stage('Deploy to Kubernetes via Helm') {
            steps {
                sh """
                helm upgrade --install python-webapp ./helm \
                --set image.repository=$DOCKER_REPO \
                --set image.tag=$IMAGE_TAG
                """
            }
        }
    }
}
