pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build & Test') {
            steps {
                dir('backend') {
                    sh '''
                        set -eu
                        java -version
                        mvn --version
                        mvn -B -ntp clean verify
                    '''
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    sh '''
                        set -eu
                        node --version
                        npm --version
                        npm ci
                        npm run build
                    '''
                }
            }
        }

        stage('Docker Compose Validation') {
            steps {
                sh '''
                    set -eu
                    export DATABASE_USERNAME="ci"
                    export DATABASE_PASSWORD="$(openssl rand -hex 32)"
                    export JWT_SECRET="$(openssl rand -hex 32)"
                    docker compose config --quiet
                '''
            }
        }

        stage('Docker Image Build') {
            steps {
                sh '''
                    set -eu
                    export DATABASE_USERNAME="ci"
                    export DATABASE_PASSWORD="$(openssl rand -hex 32)"
                    export JWT_SECRET="$(openssl rand -hex 32)"
                    docker compose build
                '''
            }
        }
    }

    post {
        success {
            echo 'CI pipeline completed successfully.'
        }
        failure {
            echo 'CI pipeline failed. Review the stage logs for details.'
        }
    }
}
