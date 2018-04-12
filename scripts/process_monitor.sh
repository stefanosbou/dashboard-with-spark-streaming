#!/bin/bash

# process_monitor.sh ----------------------------------------------------------------------------------
#
# Script Description:
#     This script will run and check every minute if all services are up and running. 
# 
# -------------------------------------------------------------------------------------------------


while true; do 
   if (( $(ps -ef | grep "io.github.stefanosbou.SparkAggregator" | grep -v grep | wc -l) == 0 )); then
      nohup java -Xms512m -Xmx512m -cp /home/ubuntu/development/currency-fair/dashboard-with-spark-streaming/project/aggregator/target/aggregator-1.0-SNAPSHOT.jar io.github.stefanosbou.SparkAggregator &
      echo "SparkAggregator process has stopped and started again"
   fi

   if (( $(ps -ef | grep "io.github.stefanosbou.ApiServer" | grep -v grep | wc -l) == 0 )); then
      nohup java -cp /home/ubuntu/development/currency-fair/dashboard-with-spark-streaming/project/api-server/target/api-server-1.0-SNAPSHOT.jar io.github.stefanosbou.ApiServer &
      echo "ApiServer process has stopped and started again"
   fi

   if (( $(ps -ef | grep "config/zookeeper.properties" | grep -v grep | wc -l) == 0 )); then
      nohup /home/ubuntu/kafka/bin/zookeeper-server-start.sh -daemon /home/ubuntu/kafka/config/zookeeper.properties > /dev/null 2>&1 &
      echo "Zookeeper process has stopped and started again"
   fi

   if (( $(ps -ef | grep "config/server.properties" | grep -v grep | wc -l) == 0 )); then
      nohup /home/ubuntu/kafka/bin/kafka-server-start.sh -daemon /home/ubuntu/kafka/config/server.properties > /dev/null 2>&1 &
      echo "Kafka process has stopped and started again"
   fi
   sleep 60
done

