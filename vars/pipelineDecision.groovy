def decidePipeline(config) {
    if (config.application == 'nodejs') {
        nodejsPipeline(config)
    } else if (config.application == 'java') {
        javaPipeline(config) // Assuming you have a similar function for Java
    } else if (config.application == 'python') {
        pythonPipeline(config) // Assuming you have a similar function for Python
    } else {
        error "Unsupported application type: ${config.application}"
    }
   
}