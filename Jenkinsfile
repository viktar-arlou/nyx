pipeline {
    agent any
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
                sh 'echo "Performing build steps..."'
                // Add your build steps here, e.g., sh 'make', sh 'npm install', etc.
            }
        }
    }
}