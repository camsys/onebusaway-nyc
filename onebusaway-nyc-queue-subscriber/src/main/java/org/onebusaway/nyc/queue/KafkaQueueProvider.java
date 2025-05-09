package org.onebusaway.nyc.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SerializationUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.onebusaway.nyc.queue.IQueueListenerTask._mapper;

public class KafkaQueueProvider implements MessageQueueProvider{

    private final BlockingQueue<QueueMessage> handoff = new LinkedBlockingQueue<>(20000);

    private volatile boolean initialized = false;

    private int processedCount = 0;

    private int _countInterval = 10000;

    private String host;
    private String queueName;
    private Integer port;
    private Date markTimestamp = new Date();

    private KafkaConsumer<String, String> _consumer;
    private Properties properties;
    private Thread readerThread;

    private static Logger _log = LoggerFactory.getLogger(KafkaQueueProvider.class);


    @Override
    public synchronized void initialize(String host,
                                        String queueName,
                                        Integer port) throws InterruptedException {
        this.host = host;
        this.queueName = queueName;
        this.port = port;

        if (initialized) {
            _log.info("Re-initializing Kafka provider - shutting down old connection first");
            shutdown();
        }

        initializeKafka();

        readerThread = new Thread(this::readIncoming, "kafka-reader");
        readerThread.start();
    }

    private void initializeKafka() {
        String bind = host + ":" + port;
        _log.info("binding to " + bind + " with topic=" + queueName);

        properties = new Properties();
        setProperties(bind, queueName);

        _consumer = new KafkaConsumer<>(properties);
        _consumer.subscribe(Arrays.asList(queueName));
        _consumer.poll(Duration.ofMillis(100));
    }

    private void setProperties(String bind, String queueName){
        try {
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, queueName);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }catch(Exception e){
            System.out.println("exception = " + e + "");
        }
    }


    private void readIncoming() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = _consumer.poll(0 * 1000);
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            String jsonValue = record.value();
                            if (jsonValue.startsWith("\"") && jsonValue.endsWith("\"")) {
                                jsonValue = jsonValue.substring(1, jsonValue.length() - 1).replace("\\\"", "\"");
                            }

                            byte[] buff = jsonValue.getBytes(StandardCharsets.UTF_8);

                            handoff.put(new QueueMessage(queueName, buff));
                            processedCount++;
                        } catch (Exception ex) {
                            _log.error("#####>>>>> processMessage() failed", ex);
                        }
                    }
                    Thread.yield();
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        _log.warn("exiting (interrupted) {}", queueName);
                        return;
                    }
                }

                if (processedCount > _countInterval) {
                    long timeInterval = (new Date().getTime() - markTimestamp.getTime());
                    _log.info("{} input queue: processed " + _countInterval + " messages in "
                            + (timeInterval / 1000)
                            + " seconds. (" + (1000.0 * processedCount / timeInterval)
                            + ") records/second", queueName);

                    markTimestamp = new Date();
                    processedCount = 0;
                }
            }
        }catch (Exception e) {
            Thread.currentThread().interrupt();
        } finally {
            _log.info("Kafka read loop exiting");
        }
    }

    @Override
    public QueueMessage nextMessage() throws InterruptedException {
        return handoff.take();
    }

    @Override
    public synchronized void shutdown() {
        if (!initialized) return;
        readerThread.interrupt();
        try {
            readerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!_consumer.subscription().isEmpty()) {
            _consumer.close();
        }
    }
}

