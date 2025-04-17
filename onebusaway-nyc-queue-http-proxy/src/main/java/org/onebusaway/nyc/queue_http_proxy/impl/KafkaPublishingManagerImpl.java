package org.onebusaway.nyc.queue_http_proxy.impl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaPublishingManagerImpl implements KafkaPublishingManager{
    private final KafkaProducer<String, String> producer;

    private static final Logger log = LoggerFactory.getLogger(KafkaPublishingManager.class);
    private final String topicName;
    private final ObjectMapper objectMapper;
    public KafkaPublishingManagerImpl(String bootstrapServers, String topicName) {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "b-1.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098," +
                "b-2.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "dev-kafka");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "SCRAM-SHA-512");
            props.put("ssl.truststore.location", "/truststore/kafka.client.truststore.jks");



        this.producer = new KafkaProducer<>(props);
        this.topicName = topicName;
        this.objectMapper = new ObjectMapper();
    }

    public KafkaPublishingManagerImpl() {
        this(
                "b-1.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098," +
                        "b-2.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098",
                "demo_java"
        );
    }

    public void sendMessage(JsonNode jsonMessage) {
        try {
            // Convert JsonNode to JSON string
            String jsonString = objectMapper.writeValueAsString(jsonMessage);

            log.info("I am a Kafka Producer");

            String bootstrapServers = "127.0.0.1:9092";

            Properties properties = new Properties();
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

            // create the producer
            KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

            // create a producer record
            ProducerRecord<String, String> producerRecord =
                    new ProducerRecord<>("demo_java", "hello world");

            // send data - asynchronous
            producer.send(producerRecord);

            // flush data - synchronous
            producer.flush();
            // flush and close producer
            producer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
        }
    }

    public static void main(String[] args) {
        try {
            log.info("I am a Kafka Producer");

            String bootstrapServers = "b-1.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9092";

            Properties properties = new Properties();
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

            // create the producer
            KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

            // create a producer record
            ProducerRecord<String, String> producerRecord =
                    new ProducerRecord<>("demo_java", "hello world");

            // send data - asynchronous
            producer.send(producerRecord);

            // flush data - synchronous
            producer.flush();
            // flush and close producer
            producer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
