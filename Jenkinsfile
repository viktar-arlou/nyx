pipeline {
    agent any
    triggers {
        giteaTrigger()
    }
    stages {
        stage('Build') {
            steps {
                sh 'echo Hello, World!'
            }
        }
    }
}