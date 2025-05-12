// Pipeline parameters for flexibility
parameters {
    string(name: 'BRANCH', defaultValue: 'master', description: 'Branch to build')
}

// Environment variables for Gitea configuration
environment {
    GITEA_URL = 'http://rover:3000/arlou.com/nyx.git'
    GITEA_CREDENTIALS = 'jenkins_gitea_pat'
}

// Configure webhook trigger for Gitea push events
triggers {
    GenericTrigger(
        genericVariables: [
            [key: 'ref', value: '$.ref'],
            [key: 'repository', value: '$.repository.name']
        ],
        causeString: 'Triggered by Gitea push to $ref',
        token: 'gitea-webhook-token',
        printContributedVariables: true,
        printPostContent: true,
        regexpFilterText: '$ref',
        regexpFilterExpression: "^refs/heads/${params.BRANCH}\$"
    )
}

// Pipeline stages
stages {
    // Stage 1: Checkout code from Gitea
    stage('Checkout') {
        steps {
            echo "Checking out branch ${params.BRANCH} from ${env.GITEA_URL}"
            git url: "${env.GITEA_URL}",
                credentialsId: "${env.GITEA_CREDENTIALS}",
                branch: "${params.BRANCH}"
        }
    }

    // Stage 2: Build the project
    stage('Build') {
        steps {
            echo 'Building the project...'
            // Replace with your build command (e.g., 'npm install && npm build')
            sh 'mvn clean install'
        }
    }

    // Stage 3: Run tests
    stage('Test') {
        steps {
            echo 'Running tests...'
            // Replace with your test command (e.g., 'npm test')
            sh 'mvn test'
        }
    }
}

// Post-build actions
post {
    always {
        echo 'Pipeline execution completed.'
    }
    success {
        echo "Build and tests for branch ${params.BRANCH} succeeded!"
    }
    failure {
        echo "Build or tests for branch ${params.BRANCH} failed. Check logs for details."
    }
}