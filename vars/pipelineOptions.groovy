def call(){
    return {
    timeout(time: 30, unit: 'MINUTES')
    disableConcurrentBuilds()
    ansiColor('xterm')
    }
}