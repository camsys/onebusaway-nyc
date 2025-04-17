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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.commons.lang.SerializationUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;

/**
 * Base class for listeners that subscribe to ZeroMQ. Provides a simple
 * re-connection mechanism if the IP changes.
 */
public abstract class KafkaQueueListenerTask implements IQueueListenerTask{

	protected static Logger _log = LoggerFactory
			.getLogger(KafkaQueueListenerTask.class);

	@Autowired
	protected ConfigurationService _configurationService;
	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	private ExecutorService _executorService = null;
	protected boolean _initialized = false;
	protected ObjectMapper _mapper = new ObjectMapper().registerModule(new JaxbAnnotationModule());

	protected DNSResolver _resolver = null;
	protected int _countInterval = 10000;
	protected KafkaConsumer<String, String> consumer = null;

	protected Properties properties = new Properties();

	public void startDNSCheckThread() {
		String host = getQueueHost();
		_resolver = new DNSResolver(host);
		if (_taskScheduler != null) {
			DNSCheckThread dnsCheckThread = new DNSCheckThread();
			// ever 10 seconds
			_taskScheduler.scheduleWithFixedDelay(dnsCheckThread, 10 * 1000);
		}
	}

	private class ReadThread implements Runnable {

		int processedCount = 0;
		Date markTimestamp = new Date();

		private KafkaConsumer<String, String> _consumer = null;

		String _topicName;

		public ReadThread(KafkaConsumer<String, String> consumer, String topicName) {
			_consumer = consumer;
			_topicName = topicName;
		}

		@Override
		public void run() {
		  _log.warn("ReadThread for queue " + getQueueName() + " starting");

			while (!Thread.currentThread().isInterrupted()) {
				// prefer a java sleep to a native block
				ConsumerRecords<String, String> records = _consumer.poll(0 * 1000); // microseconds for 2.2, milliseconds for 3.0
				if (!records.isEmpty()) {

					for (ConsumerRecord<String, String> record : records) {
						try {
							byte[] buff = SerializationUtils.serialize((Serializable) record);
							processMessage(_topicName, buff);
							processedCount++;
						} catch(Exception ex) {
							_log.error("#####>>>>> processMessage() failed", ex);
						}
					}
						
					Thread.yield();
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						_log.warn("exiting (interrupted) " + getQueueName());
						return;
					}
				}

				if (processedCount > _countInterval) {
				  long timeInterval = (new Date().getTime() - markTimestamp.getTime()); 
					_log.info(getQueueDisplayName()
							+ " input queue: processed " + _countInterval + " messages in "
							+ (timeInterval/1000) 
							+ " seconds. (" + (1000.0 * processedCount/timeInterval) 
							+ ") records/second");

					markTimestamp = new Date();
					processedCount = 0;
				}

			}
			_log.error("Thread loop Interrupted, exiting queue " + getQueueName());
		}
	}

	@PostConstruct
	public void setup() {
		_executorService = Executors.newFixedThreadPool(1);
		startListenerThread();
		startDNSCheckThread();
		_log.warn("threads started for queue " + getQueueName());
	}

	@PreDestroy
	public void destroy() {
		_log.info("destroy " + getQueueName());
		_executorService.shutdownNow();
		if (_taskScheduler != null)
			_taskScheduler.shutdown();
	}

	protected void reinitializeQueue() {
		try {
			initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
		} catch (InterruptedException ie) {
			return;
		}
	}

	// (re)-initialize Kafka listener with the given args
	protected synchronized void initializeQueue(String host, String queueName,
			Integer port) throws InterruptedException {
		String bind = host + ":" + port;
		_log.warn("binding to " + bind + " with topic=" + queueName);

		consumer = new KafkaConsumer<>(properties);

		if (properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG).isEmpty()) {
			setProperties(bind, queueName);
		}

		if (!consumer.subscription().isEmpty()) {
			_executorService.shutdownNow();
			Thread.sleep(1 * 1000);
			_log.debug("_executorService.isTerminated="
					+ _executorService.isTerminated());
			consumer.close();
			_executorService = Executors.newFixedThreadPool(1);
		}

		consumer.subscribe(Arrays.asList(queueName));

		consumer.poll(Duration.ofMillis(100));

		_executorService.execute(new ReadThread(consumer, bind));

		_log.warn("queue " + queueName + " is listening on " + bind);
		_initialized = true;

	}

	private class DNSCheckThread extends TimerTask {

		@Override
		public void run() {
			try {
				if (_resolver.hasAddressChanged()) {
					_log.warn("Resolver Changed -- re-binding queue connection");
					reinitializeQueue();
				}
			} catch (Exception e) {
				_log.error(e.toString());
				_resolver.reset();
			}
		}
	}

	private void setProperties(String bind, String queueName){

		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, queueName);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

	}

}