pipeline {
  agent {
    docker {
      image 'maven:3-alpine'
      args '''-p 3000:3000
-v /root/.m2:/root/.m2'''
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'cd project/common'
        sh 'mvn clean install'
      }
    }
  }
}