pipeline {

    // Use Dockerfile in the *same directory* as Jenkinsfile
    agent {
        dockerfile {
            filename 'Dockerfile.jenkins-agent'
            // For Windows Docker Desktop socket support (optional)
            // args '-v //./pipe/docker_engine:/var/run/docker.sock:/var/run/docker.sock'
        }
    }

    stages {

        stage('Show Workspace Files') {
            steps {
                sh '''
                    echo "=== Listing Workspace Files ==="
                    ls -l
                '''
            }
        }

        stage('Verify Tools Inside Docker Agent') {
            steps {
                sh '''
                    echo "=== Checking Tools ==="

                    echo "--- Docker CLI ---"
                    docker --version || echo "Docker client inside container - OK (will not run daemon)"

                    echo "--- Git ---"
                    git --version

                    echo "--- Python ---"
                    python3 --version || python --version

                    echo "--- Kubectl ---"
                    kubectl version --client

                    echo "--- Helm ---"
                    helm version

                    echo "--- gcloud ---"
                    gcloud version

                    echo "=== Tool Check Completed ==="
                '''
            }
        }

    }
}
