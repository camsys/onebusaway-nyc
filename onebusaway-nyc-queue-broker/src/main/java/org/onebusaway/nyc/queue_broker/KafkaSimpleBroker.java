package org.onebusaway.nyc.queue_broker;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Simple message broker implemented using Kafka.
 * Consumes messages from an input topic and republishes them to an output topic.
 */
public class KafkaSimpleBroker {
  private static final Logger logger = LoggerFactory.getLogger(KafkaSimpleBroker.class);

  // Default configuration values
  private static final int DEFAULT_IN_PORT = 9092;
  private static final int DEFAULT_OUT_PORT = 9092;
  private static final String DEFAULT_SERVER = "localhost";

  private int inPort;
  private int outPort;
  private String server;
  protected Properties properties = new Properties();

  protected KafkaConsumer<String, String> consumer;

  protected KafkaProducer<String, String> producer;

  public KafkaSimpleBroker() {
    this(DEFAULT_IN_PORT, DEFAULT_OUT_PORT, DEFAULT_SERVER);
  }

  public KafkaSimpleBroker(int inPort, int outPort, String server) {
    this.inPort = inPort;
    this.outPort = outPort;
    this.server = server;
    logger.info("Starting up KafkaSimpleBroker with input port {} and output port {}", inPort, outPort);
    logger.info("Kafka bootstrap servers: {}", server);
  }

  /**
   * Main method to run the Kafka broker
   * @param args Optional arguments: [input-port] [output-port]
   */
  public static void main(String[] args) {
    KafkaSimpleBroker broker = new KafkaSimpleBroker();

    // Override default ports if arguments are provided
    if (args.length > 0) {
      broker.setInPort(Integer.parseInt(args[0]));
    }
    if (args.length > 1) {
      broker.setOutPort(Integer.parseInt(args[1]));
    }
    if (args.length > 2) {
      broker.setServer(args[2]);
    }

    broker.run();
  }

  /**
   * Run the Kafka broker, creating a consumer and producer
   */
  public void run() {
    // Create a unique topic name based on input port
    String inputTopic = "input-port-" + inPort;
    String outputTopic = "output-port-" + outPort;
    String inBind = "tcp://" + server + ":" + inPort;
    String outBind = "tcp://" + server + ":" + outPort;

    if (properties.isEmpty() ||
            properties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isBlank() ||
            properties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isEmpty()) {

      properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, outBind);
      properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, inBind);
      properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, inputTopic);
      properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    }

    producer = new KafkaProducer<>(properties);
    consumer = new KafkaConsumer<>(properties);

      // Subscribe to input topic
      consumer.subscribe(Collections.singletonList(inputTopic));
      logger.info("Subscribed to input topic: {}", inputTopic);
      logger.info("Will publish to output topic: {}", outputTopic);

      // Continuously poll for messages
      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<String, String> record : records) {
          try {
            // Republish each message to the output topic
            ProducerRecord<String,String> producerRecord =
                    new ProducerRecord<>(outputTopic, record.key(), record.value());

            producer.send(producerRecord, (metadata, exception) -> {
              if (exception != null) {
                logger.error("Error sending message to output topic", exception);
              }
            });
          } catch (Exception e) {
            logger.error("Error processing message", e);
          }
        }
      }
    }
  public void setInPort(int inPort) {
    this.inPort = inPort;
  }

  public void setOutPort(int outPort) {
    this.outPort = outPort;
  }

  public void setServer(String server) {
    this.server = server;
  }
}
