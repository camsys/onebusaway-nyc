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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.TimerTask;
import java.util.concurrent.*;

public abstract class QueueListenerTask implements IQueueListenerTask {
	private final BlockingQueue<QueueMessage> processingQueue =
			new LinkedBlockingQueue<>(10000);
	private ExecutorService executor;

	private MessageQueueProvider queueProvider;

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;

	@Autowired
	protected ConfigurationService _configurationService;

	@Lookup
	protected MessageQueueProvider getMessageQueueProvider(){
		return queueProvider;
	};

	protected DNSResolver _resolver = null;

	protected boolean _initialized = false;

	protected ObjectMapper _mapper = new ObjectMapper().registerModule(new JaxbAnnotationModule());

	public abstract void startListenerThread();

	public abstract String getQueueHost();

	public abstract String getQueueName();

	public abstract boolean processMessage(String address, byte[] payload) throws Exception;

	/**
	 * Return the name of the queue for display of statistics in logs.
	 */
	public abstract String getQueueDisplayName();

	public abstract Integer getQueuePort();

	@PostConstruct
	public void setup() {
		queueProvider = getMessageQueueProvider();
		startListenerThread();
		startDNSCheckThread();
	}

	@Override
	public synchronized void initializeQueue(String host, String queueName,
											 Integer port){
		try {
			queueProvider.initialize(host, queueName, port);

			if (executor != null && !executor.isShutdown()) {
				executor.shutdownNow();
			}

			ThreadFactory threadFactory = new ThreadFactoryBuilder()
					.setNameFormat("queue-%d").build();
			executor = Executors.newFixedThreadPool(1, threadFactory);

			executor.execute(() -> {
				Thread.currentThread().setName("processor-" + getQueueName());
				try {
					while (!Thread.currentThread().isInterrupted()) {
						QueueMessage msg = queueProvider.nextMessage();
						try {
							processMessage(msg.getTopic(), msg.getPayload());
						} catch (Exception ex) {
							_log.error("Error processing message", ex);
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			_initialized = true;

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void reinitializeQueue() {
		initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
	}

	public void startDNSCheckThread() {
		String host = getQueueHost();
		_resolver = new DNSResolver(host);
		if (_taskScheduler != null) {
			DNSCheckThread dnsCheckThread = new DNSCheckThread();
			// ever 10 seconds
			_taskScheduler.scheduleWithFixedDelay(dnsCheckThread, 10 * 1000);
		}
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

	@PreDestroy
	public void destroy() {
		_log.info("destroy " + getQueueName());
		_executorService.shutdownNow();
		if (_taskScheduler != null) {
			_taskScheduler.shutdown();
		}
		if(queueProvider != null) {
			queueProvider.shutdown();

		}
	}

}
