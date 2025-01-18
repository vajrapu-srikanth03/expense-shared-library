def call(config) {

pipeline {
    agent any

    tools {
        nodejs 'nodejs20'  // Node.js tool
        git 'git'  // Git tool
        snyk 'snyk' // snyk tool
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        ansiColor('xterm')
    } 

    environment {
        
        DEBUG = 'true'
        appVersion = ''  // Example version, replace with actual value
        region = "${config.region ?: 'us-east-1'}"
        account_id = '608782704145'
        project = 'expense'
        environment = 'dev'
        component = "${config.component}"
        customImage = ''
        SONAR_HOME = tool 'sonar-6.2' // scanner configuration servername and scanner name both should be same
        //PATH = "/usr/bin:${env.PATH}"  // Force the pipeline to use /usr/bin/git
    }
    
    stages {
        stage('checkout') {
            steps {
                script {
                    common.checkout(config)
                }
            }
        }
        stage('Read Version') {
            steps {
                script {
                    common.readVersion("nodejs")
                }
            }
        }
        stage('Code Quality') {
            steps {
                script {
                    common.codeQuality()
                }
            }
        }
        stage('Quality Gate') {
            steps {
                script {
                    common.qualityGate()
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    common.build("nodejs")
                }
            }
        }
        stage('Nexus Upload') {
            steps {
                script {
                    common.nexusUpload()
                }
            }
        }
        stage('Install Dependencies') {
            steps {
                script {
                    common.installDependencies("nodejs")
                }
            }
        }
        // stage('Dependency Check OWASP') {
        //     steps {
        //         script {
        //             common.dependencyCheck()
        //         }
        //     }
        // }
        stage('Snyk Check') {
            steps {
                script {
                    common.snykCheck()
                }
            }
        }
        stage('Trivy FS Scan') {
            steps {
                script {
                    common.trivyfsScan()
                }
            }
        }
        stage('NPM Build') {
            steps {
                script {
                    common.npmbuild("nodejs")
                }
            }
        }
        stage('Docker Build') {
            steps {
                script {
                    common.dockerBuild()
                }
            }
        }
        stage('Docker Scout') {
            steps {
                script {
                    common.dockerScout()
                }
            }
        }
        stage('Trivy Image Scan') {
            steps {
                script {
                    common.trivyImageScan()
                }
            }
        }
        stage('Push Image to Dockerhub') {
            steps {
                script {
                    common.pushImagetoDockerhub()
                }
            }
        }
        stage('Push Image to AWS ECR') {
            steps {
                script {
                    common.pushImagetoAWSECR()
                }
            }
        }
    }

    post {
        always {
            echo "Cleaning workspace..."
            cleanWs()
            deleteDir()
        }
        success {
            echo "Pipeline was successful!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}

}