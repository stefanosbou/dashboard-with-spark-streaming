pipeline {
  agent {
    docker {
      image 'maven:3-alpine'
      args '-p 3000:3000 -v /root/.m2:/root/.m2'
    }

  }
  stages {
    stage('Install Common') {
      steps {
        sh 'cd project/common/; mvn clean install'
      }
    }
    stage('Build Api Server') {
      steps {
        sh 'cd project/api-server/; mvn clean package'
      }
    }
    stage('Build Aggregator') {
      steps {
        sh 'cd project/aggregator/; mvn clean package'
      }
    }
  }
}