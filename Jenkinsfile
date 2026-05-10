pipeline {
    agent any

    // ── Build Parameters ─────────────────────────────────────────────
    parameters {
        string(
            name: 'EXAM_DATE',
            defaultValue: '2024-12-15',
            description: 'Exam date for seating plan (YYYY-MM-DD)'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Deployment target environment'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit tests before building?'
        )
    }

    environment {
        APP_NAME    = 'exam-seating'
        JAVA_HOME   = tool 'JDK-17'
        MAVEN_HOME  = tool 'Maven-3.9'
        IMAGE_TAG   = "${params.EXAM_DATE}-build-${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                echo "🔄 Checking out source for exam date: ${params.EXAM_DATE}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('backend') {
                    sh "${MAVEN_HOME}/bin/mvn clean package -DskipTests"
                    echo "✅ Fat JAR built with exam date: ${params.EXAM_DATE}"
                }
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                dir('backend') {
                    sh "${MAVEN_HOME}/bin/mvn test"
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${APP_NAME}:${IMAGE_TAG} ./backend
                    docker tag ${APP_NAME}:${IMAGE_TAG} ${APP_NAME}:latest
                """
            }
        }

        stage('Deploy') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                echo "🚀 Deploying to production for exam: ${params.EXAM_DATE}"
                sh 'docker compose up -d --build'
            }
        }

    }

    post {
        success {
            echo "✅ Pipeline SUCCESS — Seating plan ready for ${params.EXAM_DATE}"
        }
        failure {
            echo "❌ Pipeline FAILED — Check console output"
            // mail to: 'exam-dept@college.edu', subject: "Build Failed for ${params.EXAM_DATE}"
        }
        always {
            cleanWs()
        }
    }
}
