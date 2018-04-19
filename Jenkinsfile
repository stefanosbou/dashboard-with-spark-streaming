pipeline {
  agent {
    docker {
      image 'ubuntu'
      args '-p 3000:3000'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh '''cd /home/ubuntu/development/currency-fair/dashboard-with-spark-streaming/project/common
; mvn clean install'''
      }
    }
  }
}