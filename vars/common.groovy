// Define appVersion as a global variable
//def appVersion

def checkout(config) {
    stage('checkout code') {
        cleanWs() //clean the workspace
            // for public repository
        git branch: 'main', url: "${config.REPO_URL}"
    }
}

def readVersion(appType) {
    stage('Read the version') {
        if(appType=="nodejs"){
            script {
                def props = readJSON file: 'package.json'
                appVersion = props.version
                echo "App version: ${appVersion}"
            }
        }
    }

}

def codeQuality(){
    stage('SonarQube Code Analysis'){
        withSonarQubeEnv('sonar'){ //server name
            sh '$SONAR_HOME/bin/sonar-scanner'
            //generic scanner, it automatically understands the language and provide scan results
        }
    }    
}

def qualityGate(){
    stage('Quality Gate') {
        timeout(time: 5, unit: 'MINUTES') {
        waitForQualityGate abortPipeline: true, credentialsId: 'Sonar-token'
        }
    }
}

def build(appType){
    stage('Build') {
        if(appType=="nodejs"){
            sh "zip -q -r ${JOB_BASE_NAME}.zip Dockerfile package.json TransactionService.js index.js schema/ -x '.git' -x '*.gitignore' -x '*.zip'"
        }
    } 
}

def nexusUpload() {
    stage('Nexus Upload') {
        script {
            nexusArtifactUploader(
                nexusVersion: 'nexus3',
                protocol: 'http',
                nexusUrl: pipelineGlobals.nexusURL(),
                groupId: 'com.expense',
                version: "${appVersion}",
                repository: "${JOB_BASE_NAME}",
                credentialsId: 'nexus-auth',
                artifacts: [
                    [artifactId: "${JOB_BASE_NAME}",
                    classifier: '',
                    file: "${JOB_BASE_NAME}.zip",
                    type: 'zip']
                ]
            )
        }
    }
}

def installDependencies(appType) {
    stage('Install Dependencies') {
        if(appType=="nodejs"){
            sh 'npm install'
        }
    }
}

def dependencyCheck() {
    stage("OWASP Dependency Check"){
        dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
        dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
    }
}

def snykCheck() {
    stage("Dependency Check using Snyk"){
        snykSecurity(
            organisation: 'vajrapu-srikanth03',
            projectName: 'backend',
            snykInstallation: 'snyk',
            snykTokenId: 'snyk-token',
            targetFile: 'package.json',
        )
    }
}

def trivyfsScan() {
    stage("Trivy FS Scan"){
        sh "trivy fs --format table -o trivy-fs-report.html ."
    } 
}


def npmbuild(appType) {
    stage('npm Build') {
        script {
            if(appType=="nodejs"){
                sh 'npm run build' // Build the Node.js project
            }
        }
    }
}



def dockerBuild(){
    stage('Docker build') {
        script {
            customImage = docker.build("srikanthhg/$JOB_BASE_NAME:${appVersion}")
        }
    }
}

def dockerScout(){
    stage('Docker Scout Image'){
        script {
            withDockerRegistry(credentialsId: 'docker-auth', toolName: 'docker'){
            sh "docker-scout cves srikanthhg/$JOB_BASE_NAME:${appVersion}"
            
            }
            //--exit-code --only-severity critical,high
        }
    }
}

def trivyImageScan(){
    stage('Image scan') {
        sh "trivy image --format table srikanthhg/$JOB_BASE_NAME:${appVersion}"
    }
}

def pushImagetoDockerhub(){
    stage('push image to dockerhub') {
        script {
            withDockerRegistry(credentialsId: 'docker-auth', toolName: 'docker') {
            customImage.push()
            }
        }
    }
}

def pushImagetoAWSECR(){
    stage('push image to AWS ECR') {
        script {
            withAWS(region: 'us-east-1', credentials: 'aws-ecr') {
                sh "aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com"
                sh "docker tag srikanthhg/$JOB_BASE_NAME:${appVersion} ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/$JOB_BASE_NAME:${appVersion}"
                sh "docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/$JOB_BASE_NAME:${appVersion}"
                
                sh "aws ecr describe-image-scan-findings --repository-name ${project}/$JOB_BASE_NAME --image-id imageTag=${appVersion}  --region ${region} "
                
                scanResults = sh(script: """ aws ecr describe-image-scan-findings --repository-name ${project}/$JOB_BASE_NAME --image-id imageTag=${appVersion}  --region ${region} --query "imageScanFindings.findings" """, returnStdout: true).trim()

                echo "Scan Results: ${scanResults}"
                if (scanResults.contains("HIGH")) {
                    currentBuild.result = 'FAILURE'
                    error("High vulnerabilities found!")
                }
            }
        }
    }
}