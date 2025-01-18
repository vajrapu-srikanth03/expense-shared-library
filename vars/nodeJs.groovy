def call(config) {
    //common.checkout(config)                    // Checkout code
    
    //common.readVersion("nodejs")         // Read the version from package.json
    
    //common.codeQuality()                 // Run SonarQube code analysis
    //common.qualityGate()                 // Quality Gate check
    
    //common.build("nodejs")               // Create zip of the build artifacts
    //common.nexusUpload()

    //common.installDependencies("nodejs") // Install dependencies for Node.js
    //common.dependencyCheck()             // OWASP dependency check
    //common.snykCheck()                   // Run Snyk for vulnerabilities
    
    //common.trivyfsScan()                 // Run Trivy filesystem scan
    //common.npmbuild("nodejs")            // Build the Node.js project
    
    //common.dockerBuild()                 // Build Docker image
    //common.dockerScout()                 // Run Docker Scout for vulnerabilities
    //common.trivyImageScan()              // Run Trivy image scan
    common.pushImagetoDockerhub()        // Push Docker image to Docker Hub
    common.pushImagetoAWSECR()           // Push Docker image to AWS ECR
}

