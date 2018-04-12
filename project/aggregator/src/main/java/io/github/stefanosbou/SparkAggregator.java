package io.github.stefanosbou;

import io.github.stefanosbou.helpers.database.DB;
import io.github.stefanosbou.kafka.custom.message.CustomMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.net.UnknownHostException;
import java.util.*;

import static io.github.stefanosbou.helpers.Config.KAFKA_TOPIC;
import static io.github.stefanosbou.helpers.RethinkDbHelper.COUNTRY_STATS;
import static io.github.stefanosbou.helpers.RethinkDbHelper.ORDERS_PER_CURRENCY_PAIR;
import static io.github.stefanosbou.helpers.RethinkDbHelper.TOTAL_ORDERS;

public class SparkAggregator {

   private static JavaInputDStream<ConsumerRecord<Long, CustomMessage.Order>> createKafkaStream(JavaStreamingContext jssc) throws UnknownHostException {
      Map<String, Object> kafkaParams = new HashMap<>();

      kafkaParams.put("bootstrap.servers", "localhost:9092");
      kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");
      kafkaParams.put("value.deserializer", "io.github.stefanosbou.kafka.custom.message.OrderDeserializer");
      kafkaParams.put("group.id", "kafka-spark-aggregator-" + UUID.randomUUID().toString());
      kafkaParams.put("auto.offset.reset", "earliest");
      kafkaParams.put("enable.auto.commit", "false");

      Set<String> topicsSet = new HashSet<>(Arrays.asList(KAFKA_TOPIC));

      return KafkaUtils.createDirectStream(
         jssc,
         LocationStrategies.PreferConsistent(),
         ConsumerStrategies.<Long, CustomMessage.Order>Subscribe(topicsSet, kafkaParams));
   }

   private static JavaPairDStream<String, Integer> calculateTopCountries(JavaDStream<CustomMessage.Order> stream) {
      return stream
         // Take every order and return Tuple with (country,1)
         .mapToPair(new PairFunction<CustomMessage.Order, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(CustomMessage.Order order) {
               return new Tuple2<>(order.getOriginatingCountry(), 1);
            }
         })
         .reduceByKey(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer first, Integer second) {
               return first + second;
            }
         });
   }

   private static JavaPairDStream<String, Integer> calculateTotalOrders(JavaDStream<CustomMessage.Order> stream) {
      return stream
         // Take every order and return Tuple with ("order",1)
         .mapToPair(new PairFunction<CustomMessage.Order, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(CustomMessage.Order order) {
               return new Tuple2<>("order", 1);
            }
         })
         .reduceByKey(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer first, Integer second) {
               return first + second;
            }
         });
   }

   private static JavaPairDStream<String, Integer> calculateOrdersPerCurrencyPair(JavaDStream<CustomMessage.Order> stream) {
      return stream
         // Take every order and return Tuple with (<currencyFrom/currencyTo>, 1)
         .mapToPair(new PairFunction<CustomMessage.Order, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(CustomMessage.Order order) {
               String currencyPair = order.getCurrencyFrom() + "/" + order.getCurrencyTo();
               return new Tuple2<>(currencyPair, 1);
            }
         })
         .reduceByKey(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer first, Integer second) {
               return first + second;
            }
         });
   }

   public static void main(String[] args) throws Exception {
      DB.initialize();
      SparkConf sparkConf = new SparkConf().setAppName("Spark-Aggregator").setMaster("local[*]");
      JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));
      jssc.sparkContext().setLogLevel("ERROR");

      JavaInputDStream<ConsumerRecord<Long, CustomMessage.Order>> kafkaStream = createKafkaStream(jssc);

      // Read value of each message from Kafka and return it
      JavaDStream<CustomMessage.Order> ordersStream = kafkaStream.map(new Function<ConsumerRecord<Long, CustomMessage.Order>, CustomMessage.Order>() {
         @Override
         public CustomMessage.Order call(ConsumerRecord<Long, CustomMessage.Order> kafkaRecord) throws Exception {
            return kafkaRecord.value();
         }
      });

      JavaPairDStream<String, Integer> countryStats = calculateTopCountries(ordersStream);
      saveToDB(countryStats, COUNTRY_STATS);
      JavaPairDStream<String, Integer> totalOrders = calculateTotalOrders(ordersStream);
      saveToDB(totalOrders, TOTAL_ORDERS);
      JavaPairDStream<String, Integer> ordersPerCurrencyPair = calculateOrdersPerCurrencyPair(ordersStream);
      saveToDB(ordersPerCurrencyPair, ORDERS_PER_CURRENCY_PAIR);

      // Start the computation
      jssc.start();
      jssc.awaitTermination();
   }

   private static void saveToDB(JavaPairDStream<String, Integer> stream, String table) {
      stream.foreachRDD(rdd -> {
         Map<String, Integer> rddAsMap = rdd.collectAsMap();
         if (!rddAsMap.isEmpty()) {
            DB.insertStats(table, rddAsMap);
         }
      });
   }
}
