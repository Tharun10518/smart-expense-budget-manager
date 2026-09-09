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

        stage('Deploy Application') {
            steps {
                withCredentials([
                    string(credentialsId: 'postgres-password', variable: 'DATABASE_PASSWORD'),
                    string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')
                ]) {
                    sh '''
                        set -eu
                        export DATABASE_USERNAME="postgres"
                        docker compose -p smart-expense-budget-manager up -d
                    '''
                }
            }
        }

        stage('Deployment Health Check') {
            steps {
                sh '''
                    set -eu
                    sleep 10
                    status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' http://localhost:8080/actuator/health || true)"
                    echo "Deployment health check HTTP status: ${status}"
                    test "${status}" = "200"
                '''
            }
        }
    }

    post {
        success {
            echo 'CI/CD pipeline completed successfully.'
        }
        failure {
            echo 'CI/CD pipeline failed. Review the stage logs for details.'
        }
    }
}
