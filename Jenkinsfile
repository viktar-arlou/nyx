pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref'],
                [key: 'commit_sha', value: '$.after']
            ],
            causeString: 'Triggered by Gitea webhook on $ref',
            token: 'gitea-webhook-token',
            tokenCredentialId: '',
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: false,
            regexpFilterText: '$ref',
            regexpFilterExpression: 'refs/heads/.*'
        )
    }
    stages {
        stage('Build') {
            steps {
                checkout scm
                echo "Building branch: ${ref}"
                echo "Commit SHA: ${commit_sha}"
                withMaven(
                    maven: 'Maven 3.9.x', // Name of Maven installation in Global Tool Configuration
                    jdk: 'JDK 21'     // Name of JDK installation in Global Tool Configuration
                ) {
                    sh 'mvn clean install'
                }
                // Add your build steps here, e.g., sh 'make', sh 'npm install', etc.
            }
        }
    }
}