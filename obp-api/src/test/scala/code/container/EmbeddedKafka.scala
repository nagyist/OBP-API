package code.container

import code.api.v5_0_0.V500ServerSetup
import code.setup.DefaultUsers
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.Ignore
import org.testcontainers.kafka.KafkaContainer

import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters._


@Ignore
class EmbeddedKafka extends V500ServerSetup with DefaultUsers {

  val kafkaContainer: KafkaContainer = new KafkaContainer("apache/kafka-native:3.8.0")
  // It registers a shutdown hook, which is a block of code (or function) that runs when the application terminates,
  // - either normally(e.g., when the main method completes)
  // - or due to an external signal(e.g., Ctrl + C or termination by the operating system).
  sys.addShutdownHook {
    kafkaContainer.stop()
  }
  override def beforeAll(): Unit = {
    super.beforeAll()
    // Start RabbitMQ container
    kafkaContainer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    kafkaContainer.stop()
  }

  feature(s"test EmbeddedKafka") {
    scenario("Publish and Consume Message") {

      val bootstrapServers: String = kafkaContainer.getBootstrapServers

      // Kafka producer properties
      val producerProps = new Properties()
      producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

      // Kafka consumer properties
      val consumerProps = new Properties()
      consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group")
      consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
      consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
      consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      // Create a producer
      val producer = new KafkaProducer[String, String](producerProps)
      val topic = "test-topic"
      val key = "test-key"
      val value = "Hello, Kafka!"

      // Produce a message
      producer.send(new ProducerRecord[String, String](topic, key, value))
      producer.close()

      // Create a consumer
      val consumer = new KafkaConsumer[String, String](consumerProps)
      consumer.subscribe(Collections.singletonList(topic))

      // Consume the message
      val records = consumer.poll(5000L)
      consumer.close()

      val messages = records.asScala.map(record => record.value())
      messages should contain(value)

    }
  }

}