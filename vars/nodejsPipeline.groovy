def call(config) {

pipeline {
    agent any
    tools {
        nodejs 'nodejs20'  // Node.js tool
        git 'git'  // Git tool
        snyk 'snyk' // snyk tool
    }
    options pipelineOptions()

    environment {
        
        DEBUG : 'true'
        appVersion : ''  // Example version, replace with actual value
        region : 'us-east-1'
        account_id : '608782704145'
        project : 'expense'
        environment : 'dev'
        component : ${config.component}
        def customImage : ''
        SONAR_HOME= tool 'sonar-6.2' // scanner configuration servername and scanner name both should be same
        //PATH = "/usr/bin:${env.PATH}"  // Force the pipeline to use /usr/bin/git
    }
    
    stages {
        stage('decide pipeline') {
            steps {
                script {
                    nodeJs(config)
                }
            }
        }
    }

    post {
        always {
            echo "Cleaning workspace..."
            cleanWs()
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