package org.onebusaway.nyc.queue;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.map.ObjectMapper;
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
	protected ObjectMapper _mapper = new ObjectMapper();

	protected DNSResolver _resolver = null;
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
				_zmqPoller.poll(0 * 1000); // microseconds for 2.2, milliseconds for 3.0
				if (_zmqPoller.pollin(0)) {

					String address = new String(_zmqSocket.recv(0));
					byte[] buff = _zmqSocket.recv(0);

					try {
						processMessage(address, buff);
						processedCount++;

					} catch(Exception ex) {
						_log.error("#####>>>>> processMessage() failed, exception was: " + ex.getMessage(), ex);
					}
						
					Thread.yield();
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						_log.warn("exiting...");
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
			_log.error("Thread loop Interrupted, exiting");
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
		_executorService.shutdownNow();
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

}