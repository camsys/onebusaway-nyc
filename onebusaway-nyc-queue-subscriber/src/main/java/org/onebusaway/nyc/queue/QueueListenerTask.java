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

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.zeromq.ZMQ;

/**
 * Base class for listeners that subscribe to ZeroMQ. Provides a simple
 * re-connection mechanism if the IP changes.
 */
public abstract class QueueListenerTask {

	protected static Logger _log = LoggerFactory
			.getLogger(QueueListenerTask.class);
	@Autowired
	protected ConfigurationService _configurationService;
	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	private ExecutorService _executorService = null;
	protected boolean _initialized = false;
	protected ObjectMapper _mapper = new ObjectMapper().registerModule(new JaxbAnnotationModule());

	protected LeadershipElectionResolver _resolver = null;
	protected ZMQ.Context _context = null;
	protected ZMQ.Socket _socket = null;
	protected ZMQ.Poller _poller = null;
	protected int _countInterval = 10000;

	public abstract boolean processMessage(String address, byte[] buff) throws Exception;

	public abstract void startListenerThread();

	public abstract String getQueueHost();

	public abstract String getQueueName();
	
	/**
	 * Return the name of the queue for display of statistics in logs.
	 */
	public abstract String getQueueDisplayName();

	public abstract Integer getQueuePort();

	public void setCountInterval(int countInterval) {
	  this._countInterval = countInterval;  
	}

	private class ReadThread implements Runnable {

		int processedCount = 0;

		Date markTimestamp = new Date();

		private ZMQ.Socket _zmqSocket = null;

		private ZMQ.Poller _zmqPoller = null;

		public ReadThread(ZMQ.Socket socket, ZMQ.Poller poller) {
			_zmqSocket = socket;
			_zmqPoller = poller;
		}

		@Override
		public void run() {
		  _log.warn("ReadThread for queue " + getQueueName() + " starting");

			while (!Thread.currentThread().isInterrupted()) {
				// prefer a java sleep to a native block
				_zmqPoller.poll(0 * 1000); // microseconds for 2.2, milliseconds for 3.0
				if (_zmqPoller.pollin(0)) {

					String address = new String(_zmqSocket.recv(0));
					byte[] buff = _zmqSocket.recv(0);

					try {
						processMessage(address, buff);
						processedCount++;
					} catch(Exception ex) {
						_log.error("#####>>>>> processMessage() failed", ex);
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

	// (re)-initialize ZMQ with the given args
	protected synchronized void initializeQueue(String host, String queueName,
			Integer port) throws InterruptedException {
		String bind = "tcp://" + host + ":" + port;
		_log.warn("binding to " + bind + " with topic=" + queueName);

		if (_context == null) {
			_context = ZMQ.context(1);
		}

		if (_socket != null) {
			_executorService.shutdownNow();
			Thread.sleep(1 * 1000);
			_log.debug("_executorService.isTerminated="
					+ _executorService.isTerminated());
			_socket.close();
			_executorService = Executors.newFixedThreadPool(1);
		}
		_socket = _context.socket(ZMQ.SUB);
		_poller = _context.poller(2);
		_poller.register(_socket, ZMQ.Poller.POLLIN);
		
		_socket.connect(bind);
		_socket.subscribe(queueName.getBytes());

		_executorService.execute(new ReadThread(_socket, _poller));

		_log.warn("queue " + queueName + " is listening on " + bind);
		_initialized = true;

	}


}