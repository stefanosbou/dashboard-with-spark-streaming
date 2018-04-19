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
        pwd(tmp: true)
        sh 'cd project/common'
        sh 'mvn clean install'
      }
    }
  }
}