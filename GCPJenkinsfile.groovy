pipeline {

    agent any

    environment {
        IMAGE_NAME = "balamaheshward/balamahwaer"
        BUILD_TAG = "${IMAGE_NAME}:${BUILD_NUMBER}"
        TERRAFORM_DIRS = "terraform gcp-terraform"
        REPORTS_DIR = "${WORKSPACE}/reports"
        SONAR_SCANNER_HOME = "/usr/local/sonar-scanner"
    }

    stages {

        /* ---------------------------------------------------
         * CHECKOUT CODE
         * --------------------------------------------------- */
        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    credentialsId: 'Github2',
                    url: 'https://github.com/DBalamaheshwar/Devopsautomation.git'
            }
        }

        /* ---------------------------------------------------
         * INSTALL SECURITY TOOLS
         * --------------------------------------------------- */
        stage('Install Security Tools') {
            steps {
                sh """
                    echo "=== INSTALL SECURITY TOOLS ==="

                    sudo apt-get update
                    export DEBIAN_FRONTEND=noninteractive
                    sudo apt-get install -y wget curl gnupg unzip lsb-release jq npm tar gzip

                    mkdir -p ${REPORTS_DIR}

                    #############################################
                    # TERRAFORM
                    #############################################
                    if ! command -v terraform >/dev/null; then
                        TERRAFORM_VERSION="1.3.0"
                        wget https://releases.hashicorp.com/terraform/\${TERRAFORM_VERSION}/terraform_\${TERRAFORM_VERSION}_linux_amd64.zip
                        unzip terraform_\${TERRAFORM_VERSION}_linux_amd64.zip
                        sudo mv terraform /usr/local/bin/
                    fi
 

                    #############################################
                    # SYFT
                    #############################################
                    if ! command -v syft >/dev/null; then
                        curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sudo sh -s -- -b /usr/local/bin
                    fi

 
                    #############################################
                    # TERRASCAN
                    #############################################
                    if ! command -v terrascan >/dev/null; then
                        wget -q https://github.com/accurics/terrascan/releases/download/v1.13.0/terrascan_1.13.0_Linux_x86_64.tar.gz
                        sudo tar -xzf terrascan_1.13.0_Linux_x86_64.tar.gz -C /usr/local/bin
                    fi

                    #############################################
                    # SNYK
                    #############################################
                    if ! command -v snyk >/dev/null; then
                        sudo npm install -g snyk --unsafe-perm
                    fi



                    #############################################
                    # OPA (DEFAULT POLICIES)
                    #############################################
                    if ! command -v opa >/dev/null; then
                        wget https://openpolicyagent.org/downloads/latest/opa_linux_amd64
                        sudo mv opa_linux_amd64 /usr/local/bin/opa
                        sudo chmod +x /usr/local/bin/opa
                    fi

                    # Download OPA Terraform default policies
                    mkdir -p /usr/local/opa/policies
                    curl -L -o /usr/local/opa/policies/terraform.rego \
                        https://raw.githubusercontent.com/open-policy-agent/contrib/master/policy/terraform/example.rego
                """
            }
        }

        /* ---------------------------------------------------
         * TERRAFORM INIT + PLAN (ALL DIRECTORIES)
         * --------------------------------------------------- */
        stage('Terraform Init & Plan') {
            steps {
                script {
                    TERRAFORM_DIRS.split(" ").each { dir ->
                        sh """
                            echo "=== Terraform plan for ${dir} ==="
                            cd ${WORKSPACE}/${dir}
                            terraform init -input=false
                            terraform plan -out=tfplan
                            terraform show -json tfplan > tfplan.json
                        """
                    }
                }
            }
        }

        /* ---------------------------------------------------
         * OPA POLICY SCAN (ALL TERRAFORM DIRECTORIES)
         * --------------------------------------------------- */
        stage('OPA Terraform Policy Scan') {
            steps {
                script {
                    TERRAFORM_DIRS.split(" ").each { dir ->
                        def report = "reports/opa-${dir}-${BUILD_NUMBER}.json"
                        sh """
                            opa eval -f json -i ${WORKSPACE}/${dir}/tfplan.json \
                                -d /usr/local/opa/policies \
                                "data.terraform" > ${report}
                        """
                        archiveArtifacts artifacts: report
                    }
                }
            }
        }

        /* ---------------------------------------------------
         * TERRASCAN (ALL TERRAFORM DIRECTORIES)
         * --------------------------------------------------- */
        stage('Terrascan Infra Code Scan') {
            steps {
                script {
                    TERRAFORM_DIRS.split(" ").each { dir ->
                        def report = "reports/terrascan-${dir}-${BUILD_NUMBER}.json"
                        sh """
                            terrascan scan -i terraform -p ${WORKSPACE}/${dir} -o json > ${report}
                        """
                        archiveArtifacts artifacts: report
                    }
                }
            }
        }

        /* ---------------------------------------------------
         * SNYK IaC (ALL TERRAFORM DIRECTORIES)
         * --------------------------------------------------- */
        stage('Snyk Infra Scan') {
            steps {
                script {
                    TERRAFORM_DIRS.split(" ").each { dir ->
                        def report = "reports/snyk-${dir}-${BUILD_NUMBER}.json"
                        sh """
                            snyk iac test ${WORKSPACE}/${dir} --json > ${report}
                        """
                        archiveArtifacts artifacts: report
                    }
                }
            }
        }
                      

       
        /* ---------------------------------------------------
         * CLEANUP
         * --------------------------------------------------- */
        stage('Uninstall Security Tools') {
            steps {
                sh """
                    echo "=== CLEANING SECURITY TOOLS ==="
                    sudo rm -f /usr/local/bin/{terraform,trivy,syft,grype,terrascan,kubescape,opa}
                    sudo npm uninstall -g snyk || true
                    sudo rm -rf /usr/local/sonar-scanner || true
                    sudo rm -rf /usr/local/opa || true
                """
            }
        }
    }
}
