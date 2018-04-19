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
        sh 'pwd -P; mvn clean install'
      }
    }
  }
}