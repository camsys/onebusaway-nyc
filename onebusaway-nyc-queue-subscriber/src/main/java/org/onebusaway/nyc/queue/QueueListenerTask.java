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
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.onebusaway.nyc.queue.DNSResolver;
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

	protected DNSResolver _resolver = null;
	protected ZMQ.Context _context = null;
	protected ZMQ.Socket _socket = null;
	protected ZMQ.Poller _poller = null;
	protected int _countInterval = 10000;

	public abstract boolean processMessage(String address, byte[] buff) throws Exception;

	public abstract void startListenerThread();

	public abstract String getQueueHost();

	public abstract String getQueueName();

	public abstract String getQueueDisplayName();

	public abstract Integer getQueuePort();

	public void setCountInterval(int countInterval) {
		this._countInterval = countInterval;
	}

	public void startDNSCheckThread() {
		String host = getQueueHost();
		_resolver = new DNSResolver(host);
		if (_taskScheduler != null) {
			DNSCheckThread dnsCheckThread = new DNSCheckThread();
			_taskScheduler.scheduleWithFixedDelay(dnsCheckThread, 10 * 1000);
		}
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
				try {
					_zmqPoller.poll(0 * 1000);
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
				} catch (org.zeromq.ZMQException e) {
					// Handle ZMQ errors (socket may have been closed)
					if (Thread.currentThread().isInterrupted()) {
						_log.warn("ReadThread interrupted for queue " + getQueueName());
						return;
					}
					_log.error("ZMQ error in ReadThread", e);
					return;
				}
			}
			_log.warn("Thread loop Interrupted, exiting queue " + getQueueName());
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
		_log.info("destroy " + getQueueName() + " - cleaning up resources");

		// Shutdown executor and wait for termination
		if (_executorService != null) {
			_executorService.shutdownNow();
			try {
				if (!_executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					_log.warn("ExecutorService did not terminate in time");
				}
			} catch (InterruptedException e) {
				_log.warn("Interrupted while waiting for executor termination");
				Thread.currentThread().interrupt();
			}
		}

		// Shutdown task scheduler
		if (_taskScheduler != null) {
			_taskScheduler.shutdown();
		}

		// Clean up ZMQ resources
		cleanupZmqResources();
	}

	/**
	 * Clean up ZMQ socket, poller, and context
	 */
	private synchronized void cleanupZmqResources() {
		// Unregister socket from poller first
		if (_poller != null && _socket != null) {
			try {
				_poller.unregister(_socket);
				_log.debug("Socket unregistered from poller for queue " + getQueueName());
			} catch (Exception e) {
				_log.warn("Error unregistering socket from poller for queue " + getQueueName(), e);
			}
		}
		// Note: Poller in ZMQ 3.2.2 does not have a close() method
		_poller = null;

		// Close socket
		if (_socket != null) {
			try {
				_socket.close();
				_log.debug("Socket closed for queue " + getQueueName());
			} catch (Exception e) {
				_log.warn("Error closing socket for queue " + getQueueName(), e);
			}
			_socket = null;
		}

		// Terminate context
		if (_context != null) {
			try {
				_context.term();
				_log.info("Context terminated for queue " + getQueueName());
			} catch (Exception e) {
				_log.warn("Error terminating context for queue " + getQueueName(), e);
			}
			_context = null;
		}

		_initialized = false;
	}

	protected void reinitializeQueue() {
		try {
			initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
		} catch (InterruptedException ie) {
			_log.warn("Interrupted while reinitializing queue", ie);
			Thread.currentThread().interrupt();
		}
	}

	// (re)-initialize ZMQ with the given args
	protected synchronized void initializeQueue(String host, String queueName,
												Integer port) throws InterruptedException {
		String bind = "tcp://" + host + ":" + port;
		_log.warn("binding to " + bind + " with topic=" + queueName);

		// Clean up existing resources FIRST
		if (_socket != null) {
			_log.debug("Reinitializing - cleaning up existing connection");

			// 1. Shutdown executor and wait
			if (_executorService != null) {
				_executorService.shutdownNow();
				if (!_executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					_log.warn("_executorService did not terminate in time");
				}
			}

			// 2. Unregister socket from poller
			if (_poller != null) {
				try {
					_poller.unregister(_socket);
					_log.debug("Socket unregistered from old poller");
				} catch (Exception e) {
					_log.warn("Error unregistering socket from old poller", e);
				}
			}
			// Note: Poller in ZMQ 3.2.2 does not have a close() method
			_poller = null;

			// 3. Close old socket
			try {
				_socket.close();
				_log.debug("Old socket closed");
			} catch (Exception e) {
				_log.warn("Error closing old socket", e);
			}
			_socket = null;

			// 4. Create new executor
			_executorService = Executors.newFixedThreadPool(1);
		}

		// Initialize context if needed (only created once)
		if (_context == null) {
			_context = ZMQ.context(1);
		}

		// Create new socket and poller
		try {
			_socket = _context.socket(ZMQ.SUB);
			_poller = _context.poller(2);
			_poller.register(_socket, ZMQ.Poller.POLLIN);

			_socket.connect(bind);
			_socket.subscribe(queueName.getBytes());

			_executorService.execute(new ReadThread(_socket, _poller));

			_log.warn("queue " + queueName + " is listening on " + bind);
			_initialized = true;
		} catch (Exception e) {
			_log.error("Failed to initialize queue connection to " + bind, e);
			// Clean up partial initialization
			if (_poller != null && _socket != null) {
				try {
					_poller.unregister(_socket);
				} catch (Exception ex) {
					_log.warn("Error unregistering socket during cleanup", ex);
				}
			}
			_poller = null;
			if (_socket != null) {
				try {
					_socket.close();
				} catch (Exception ex) {
					_log.warn("Error closing socket during cleanup", ex);
				}
			}
			_socket = null;
			throw e;
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
				_log.error("Error in DNS check thread", e);
				_resolver.reset();
			}
		}
	}
}