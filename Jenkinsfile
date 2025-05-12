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
                // Debug: Print all environment variables to verify injection
                sh 'env | sort'
                echo "Building branch: ${env.ref ?: 'unknown'}"
                echo "Commit SHA: ${env.commit_sha ?: 'unknown'}"
                // Optional: Use workspace for local repository
                sh './mvnw install -Dmaven.repo.local=${WORKSPACE}/.m2/repository'
                sh 'echo "Performing build steps..."'
                // Add additional build steps here if needed
            }
        }
    }
}