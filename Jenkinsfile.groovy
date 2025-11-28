pipeline {
    agent any

    environment {
        IMAGE_NAME  = "balamaheshward/balamahwaer"
        BUILD_TAG   = "${IMAGE_NAME}:${BUILD_NUMBER}"
        REPORTS_DIR = "${WORKSPACE}/reports"
        TERRAFORM_DIR = "${WORKSPACE}/terraform" // Update if your Terraform code is elsewhere
    }

    stages {

        /********************
         * Checkout Code
         ********************/
        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    credentialsId: 'Github2',
                    url: 'https://github.com/DBalamaheshwar/Devopsautomation.git'
            }
        }

        /********************
         * Build Docker Image
         ********************/
        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${BUILD_TAG} ."
            }
        }

        /********************
         * Push Docker Hub
         ********************/
        stage('Push Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        docker push ${BUILD_TAG}
                    """
                }
            }
        }

        /********************
         * Terraform Apply
         ********************/
        stage('Terraform Apply') {
            steps {
                dir("${TERRAFORM_DIR}") {
                    sh """
                        terraform init
                        terraform plan
                        terraform apply -auto-approve
                    """
                }
            }
        }

        /********************
         * Prepare Reports Directory
         ********************/
        stage('Prepare Reports Directory') {
            steps {
                sh "mkdir -p ${REPORTS_DIR}"
            }
        }

        /********************
         * Install Security Tools
         ********************/
        stage('Install Security Tools') {
            steps {
                sh """
                    sudo apt-get update
                    sudo apt-get install -y wget curl gnupg lsb-release unzip python3-pip nodejs npm nmap

                    # Trivy
                    TRIVY_VERSION=\$(curl -s https://api.github.com/repos/aquasecurity/trivy/releases/latest | grep tag_name | cut -d '"' -f 4)
                    wget -q https://github.com/aquasecurity/trivy/releases/download/\$TRIVY_VERSION/trivy_\${TRIVY_VERSION#v}_Linux-64bit.deb
                    sudo dpkg -i trivy_\${TRIVY_VERSION#v}_Linux-64bit.deb

                    # Syft
                    curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sudo sh -s -- -b /usr/local/bin

                    # Grype
                    curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sudo sh -s -- -b /usr/local/bin

                    # Terrascan
                    TERRASCAN_VERSION=\$(curl -s https://api.github.com/repos/accurics/terrascan/releases/latest | grep tag_name | cut -d '"' -f 4)
                    curl -L -o terrascan_linux.tar.gz https://github.com/accurics/terrascan/releases/download/\$TERRASCAN_VERSION/terrascan_\${TERRASCAN_VERSION#v}_Linux_x86_64.tar.gz
                    tar -xzf terrascan_linux.tar.gz -C /usr/local/bin

                    # SonarQube Scanner
                    
                    SONAR_VERSION=5.12.0.23594
                    wget -q https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-\${SONAR_VERSION}-linux.zip
                    unzip sonar-scanner-cli-\${SONAR_VERSION}-linux.zip -d /opt/
                    export PATH=\$PATH:/opt/sonar-scanner-\${SONAR_VERSION}-linux/bin

                    # ZAP
                    wget -q https://github.com/zaproxy/zaproxy/releases/download/v2.12.0/ZAP_2_12_0_Linux.tar.gz
                    tar -xzf ZAP_2_12_0_Linux.tar.gz -C /opt/
                    export PATH=\$PATH:/opt/ZAP_2_12_0


                        

                    # Snyk
                    sudo npm install -g snyk

                    # ScoutSuite
                    pip3 install scoutsuite

                    # Prowler
                    git clone https://github.com/prowler-cloud/prowler.git /opt/prowler
                    chmod +x /opt/prowler/prowler

                    # Kube-bench
                    KUBE_BENCH_VERSION=\$(curl -s https://api.github.com/repos/aquasecurity/kube-bench/releases/latest | grep tag_name | cut -d '"' -f 4)
                    wget -q https://github.com/aquasecurity/kube-bench/releases/download/\$KUBE_BENCH_VERSION/kube-bench_\${KUBE_BENCH_VERSION#v}_linux_amd64.tar.gz
                    tar -xzf kube-bench_\${KUBE_BENCH_VERSION#v}_linux_amd64.tar.gz -C /usr/local/bin
                """


                echo "=== VERIFY VERSIONS ==="
                    trivy --version
                    syft --version
                    grype --version
                    terrascan version
                    sonar-scanner -v
                    zap.sh -version
                    snyk --version
                    scoutsuite --version
                    /opt/prowler/prowler -V
                    kube-bench --version
                """
            }
        }

        /********************
         * Run Application Container
         ********************/
        stage('Run Application') {
            steps {
                script {
                    sh """
                        docker run -d --name myapp -p 8080:8080 ${BUILD_TAG}
                        sleep 15
                    """
                }
            }
        }

        /********************
         * Trivy Scan
         ********************/
        stage('Trivy Scan') {
            steps {
                script {
                    def trivyReport = "${REPORTS_DIR}/trivy-report-${BUILD_NUMBER}.json"
                    sh "trivy image --format json -o ${trivyReport} ${BUILD_TAG}"
                    archiveArtifacts artifacts: trivyReport
                }
            }
        }

        /********************
         * Syft Scan
         ********************/
        stage('Syft SBOM Scan') {
            steps {
                script {
                    def syftReport = "${REPORTS_DIR}/syft-sbom-${BUILD_NUMBER}.json"
                    sh "syft ${BUILD_TAG} -o json > ${syftReport}"
                    archiveArtifacts artifacts: syftReport
                }
            }
        }

        /********************
         * Grype Scan
         ********************/
        stage('Grype Scan') {
            steps {
                script {
                    def syftReport = "${REPORTS_DIR}/syft-sbom-${BUILD_NUMBER}.json"
                    def grypeReport = "${REPORTS_DIR}/grype-report-${BUILD_NUMBER}.json"
                    sh "grype ${syftReport} -o json > ${grypeReport}"
                    archiveArtifacts artifacts: grypeReport
                }
            }
        }

        /********************
         * Terrascan Scan
         ********************/
        stage('Terrascan Scan') {
            steps {
                script {
                    def terrascanReport = "${REPORTS_DIR}/terrascan-report-${BUILD_NUMBER}.json"
                    sh "terrascan scan -f ${TERRAFORM_DIR} -o json > ${terrascanReport}"
                    archiveArtifacts artifacts: terrascanReport
                }
            }
        }

        /********************
         * SonarQube Scan
         ********************/
        stage('SonarQube Scan') {
            steps {
                script {
                    sh "sonar-scanner -Dsonar.projectKey=myproject -Dsonar.sources=. -Dsonar.host.url=http://localhost:9000 -Dsonar.login=SONAR_TOKEN"
                }
            }
        }

        /********************
         * ZAP Scan
         ********************/
        stage('ZAP Scan') {
            steps {
                script {
                    def zapReport = "${REPORTS_DIR}/zap-report-${BUILD_NUMBER}.xml"
                    sh "zap.sh -daemon -port 8090 -host 127.0.0.1 &"
                    sh "sleep 15"
                    sh "zap-cli -p 8090 quick-scan http://localhost:8080 -r ${zapReport}"
                    archiveArtifacts artifacts: zapReport
                }
            }
        }

        /********************
         * Nmap Scan
         ********************/
        stage('Nmap Scan') {
            steps {
                script {
                    def nmapReport = "${REPORTS_DIR}/nmap-report-${BUILD_NUMBER}.txt"
                    sh "nmap -sV localhost > ${nmapReport}"
                    archiveArtifacts artifacts: nmapReport
                }
            }
        }

        /********************
         * Snyk Scan
         ********************/
        stage('Snyk Scan') {
            steps {
                script {
                    def snykReport = "${REPORTS_DIR}/snyk-report-${BUILD_NUMBER}.json"
                    sh "snyk container test ${BUILD_TAG} --json > ${snykReport}"
                    archiveArtifacts artifacts: snykReport
                }
            }
        }

        /********************
         * ScoutSuite Scan
         ********************/
        stage('ScoutSuite Scan') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
                    script {
                        def scoutReport = "${REPORTS_DIR}/scoutsuite-report-${BUILD_NUMBER}"
                        sh "scout aws -r us-east-1 -o ${scoutReport}"
                        archiveArtifacts artifacts: "${scoutReport}/*"
                    }
                }
            }
        }

        /********************
         * Prowler Scan
         ********************/
        stage('Prowler Scan') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
                    script {
                        def prowlerReport = "${REPORTS_DIR}/prowler-report-${BUILD_NUMBER}.json"
                        sh "/opt/prowler/prowler -M json > ${prowlerReport}"
                        archiveArtifacts artifacts: prowlerReport
                    }
                }
            }
        }

        /********************
         * Kubernetes Context
         ********************/
        stage('Setup Kubernetes Context') {
            steps {
                script {
                    sh """
                        aws eks --region us-east-1 update-kubeconfig --name my-eks-cluster
                        kubectl get nodes
                    """
                }
            }
        }

        /********************
         * Kube-bench Scan
         ********************/
        stage('Kube-bench Scan') {
            steps {
                script {
                    def kubeBenchReport = "${REPORTS_DIR}/kube-bench-report-${BUILD_NUMBER}.json"
                    sh "kube-bench --json > ${kubeBenchReport}"
                    archiveArtifacts artifacts: kubeBenchReport
                }
            }
        }

        /********************
         * Cleanup: Stop Container
         ********************/
        stage('Cleanup Docker') {
            steps {
                sh "docker stop myapp && docker rm myapp || true"
            }
        }

        /********************
         * Cleanup Tools and Terraform Destroy
         ********************/
        stage('Cleanup Tools & Terraform Destroy') {
            steps {
                script {
                    // Terraform destroy
                    dir("${TERRAFORM_DIR}") {
                        sh "terraform destroy -auto-approve || true"
                    }

                    // Uninstall tools
                    sh """
                        sudo apt-get remove --purge -y nodejs npm nmap unzip python3-pip
                        sudo apt-get autoremove -y
                        sudo rm -rf /usr/local/bin/trivy /usr/local/bin/syft /usr/local/bin/grype /usr/local/bin/terrascan
                        sudo rm -rf /opt/sonar-scanner-* /opt/ZAP_2_12_0 /opt/prowler
                        sudo rm -rf /usr/local/bin/kube-bench
                        sudo npm uninstall -g snyk || true
                        pip3 uninstall -y scoutsuite || true
                    """
                }
            }
        }

    } // end stages
}
