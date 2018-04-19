pipeline {
  agent {
    node {
      label 'master'
    }

  }
  stages {
    stage('Install Common') {
      steps {
        sh 'cd project/common/; mvn clean install'
        sh 'ls -l /home/ubuntu/.m2'
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