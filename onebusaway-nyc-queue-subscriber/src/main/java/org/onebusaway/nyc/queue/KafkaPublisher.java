/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.queue;

import com.eaio.uuid.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

public class KafkaPublisher implements IPublisher,InitializingBean {

	private static Logger _log = LoggerFactory.getLogger(KafkaPublisher.class);
	private ExecutorService executorService = null;
	private ArrayBlockingQueue<String> outputBuffer = new ArrayBlockingQueue<String>(
			1000);
	private String topic;
	private String protocol;
	private String host;
	private int port;
	protected Properties properties = new Properties();

	protected ProducerRecord<String, String> producerRecord = null;
	protected KafkaProducer<String, String> producer = null;

	public KafkaPublisher(String topic) {
		this.topic = topic;
	}

	/**
	 * Bind Kafka to the given host and port using the specified protocol.
	 ** @param topic
	 *            zeromq topic
	 * @param protocol
	 *            "tcp" for example
	 * @param host
	 *            localhost, "*", or ip.
	 * @param port
	 *            port to bind to. Below 1024 requires elevated privs.
	 */
	public KafkaPublisher(String topic, String protocol, String host, Integer port) {
		this.topic = topic;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
	}

	public KafkaPublisher(){}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public synchronized void init() {}

	@Override
	@Transactional
	public void afterPropertiesSet() {
		//String bind = protocol + "://" + host + ":" + port;
		protocol= "http";
		host= "localhost";
		port= 9092;
		String bind = "http://localhost:9092";
		_log.warn("connecting to " + bind);
		/*
		 * do not bind to the socket, simply connect to existing socket provided by
		 * broker.
		 */
		setProperties(bind, topic);
		producer = new KafkaProducer<>(properties);
		producerRecord = new ProducerRecord<>(topic, null);
		executorService = Executors.newFixedThreadPool(1);
		executorService.execute(new SendThread(producer, topic));

	}

	/**
	 * Ask Kafka to close politely.
	 */
	@Override
	public synchronized void close() {
		_log.warn("shutting down...");
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
			Thread.sleep(1 * 1000);
		} catch (InterruptedException ie) {
			executorService.shutdownNow();
		}
		// flush and close producer
		producer.flush();
		producer.close();
	}

	@Override
	public synchronized void reset(){
		close();
		init();
	}

	/**
	 * Publish a message to a topic. Be aware that fist message may be lost as
	 * subscriber will not connect in time.
	 *
	 * @param message
	 *            the content of the message
	 */
	@Override
	public void send(byte[] message) {
		try {
			outputBuffer.put(wrap(message));
		} catch (InterruptedException ie) {
			_log.error(ie.toString());
		}
	}

	
	String wrap(byte[] message) {
		if (message == null || message.length == 0)
			return null;
		long timeReceived = getTimeReceived();
		String realtime = new String(message);
		// we remove wrapping below, so check for min length acceptable
		if (realtime.length() < 2)
			return null;

		StringBuilder prefix = new StringBuilder();
		
		prefix.append("{\"RealtimeEnvelope\": {\"UUID\":\"")
				.append(generateUUID()).append("\",\"timeReceived\": ")
				.append(timeReceived).append(",")
				.append(removeLastBracket(realtime)).append("}}");

		try{
			return RmcUtil.replaceInvalidRmcDateTime(prefix, timeReceived);
		}catch(Throwable t){
			_log.warn("unable to replace invalid rmc date", t);
			t.printStackTrace();
			return prefix.toString();
		}

	}

	@Override
	public void send(String message) {
		try {
			outputBuffer.put(wrap(message));
		} catch (InterruptedException ie) {
			_log.error(ie.toString());
		}
	}

	String wrap(String realtime) {
		if (realtime == null || realtime.length() == 0)
			return null;
		long timeReceived = getTimeReceived();

		// we remove wrapping below, so check for min length acceptable
		if (realtime.length() < 2)
			return null;

		StringBuilder prefix = new StringBuilder();

		prefix.append("{\"RealtimeEnvelope\": {\"UUID\":\"")
				.append(generateUUID()).append("\",\"timeReceived\": ")
				.append(timeReceived).append(",")
				.append(removeLastBracket(realtime)).append("}}");

		try{
			return RmcUtil.replaceInvalidRmcDateTime(prefix, timeReceived);
		}catch(Throwable t){
			_log.warn("unable to replace invalid rmc date", t);
			t.printStackTrace();
			return prefix.toString();
		}

	}


	String removeLastBracket(String s) {
		String trimmed = s.trim();
		return trimmed.substring(1, trimmed.length() - 1);
	}

	String generateUUID() {
		return new UUID().toString();
	}

	long getTimeReceived() {
		return System.currentTimeMillis();
	}

	private class SendThread implements Runnable {

		int processedCount = 0;
		Date markTimestamp = new Date();

		private KafkaProducer<String, String> _producer;

		private String _topicName;

		ProducerRecord<String, String> _producerRecord;

		public SendThread(KafkaProducer<String, String> producer, String topicName) {
			_producer = producer;
			_topicName = topicName;
		}


		public void run() {
			int errorCount = 0;
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String r = outputBuffer.take();
					_producerRecord = new ProducerRecord<>(_topicName, r);;
					producer.send(_producerRecord);
				} catch (InterruptedException ie) {
					return;
				}

				if (processedCount > 1000) {
					long timeDiff = TimeUnit.MILLISECONDS.toSeconds (System.currentTimeMillis() - markTimestamp.getTime());
					_log.info("HTTP Proxy output queue {}: processed 1000 messages in {} seconds; current queue length is {}",
								this._topicName, timeDiff, outputBuffer.size());
					markTimestamp = new Date();
					processedCount = 0;
				}

				processedCount++;
			}
		}
	}

	private void setProperties(String bind, String queueName){

		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, queueName);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

	}
}
