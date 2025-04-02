package org.onebusaway.nyc.queue_http_proxy.impl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaPublishingManagerImpl implements KafkaPublishingManager{
    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final ObjectMapper objectMapper;

    // Explicit, non-abstract constructor
    public KafkaPublishingManagerImpl(String bootstrapServers, String topicName) {
        // Kafka Broker Configuration
        Properties props = new Properties();

        // Use passed bootstrap servers
        props.put("bootstrap.servers", bootstrapServers);

        // Serialization
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());

        // Optional: Authentication Configuration
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "AWS_MSK_IAM");
        props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");

        // Create Producer
        this.producer = new KafkaProducer<>(props);

        // Set Topic Name from parameter
        this.topicName = topicName;

        // Initialize ObjectMapper for JSON processing
        this.objectMapper = new ObjectMapper();
    }

    // Alternative constructor with default parameters
    public KafkaPublishingManagerImpl() {
        this(
                "b-1.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098," +
                        "b-2.devkafka.oq7n4j.c2.kafka.us-east-1.amazonaws.com:9098",
                "test-topic"
        );
    }

    public void sendMessage(JsonNode jsonMessage) {
        try {
            // Convert JsonNode to JSON string
            String jsonString = objectMapper.writeValueAsString(jsonMessage);

            // Create Producer Record
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topicName, jsonString);

            // Send message
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Message sent successfully: " +
                            "Topic=" + metadata.topic() +
                            ", Partition=" + metadata.partition() +
                            ", Offset=" + metadata.offset());
                } else {
                    System.err.println("Failed to send message: " +
                            exception.getMessage());
                }
            });

            // Ensure message is sent
            producer.flush();
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
            // Create producer using interface
            KafkaPublishingManager kafkaProducer = new KafkaPublishingManagerImpl();

            // Example of creating a JsonNode
            JsonNode jsonMessage = new ObjectMapper().readTree(
                    "{\"key\":\"value\", \"data\":\"example\", \"nested\":{\"inner\":\"data\"}}"
            );

            // Send message
            kafkaProducer.sendMessage(jsonMessage);

            // Close resources
            kafkaProducer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
