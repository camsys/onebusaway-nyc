package org.onebusaway.nyc.queue;

import com.eaio.uuid.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulate ZeroMQ queue operations. ZeroMQ prefers to be on its own thread
 * and not be hit simultaneously, so synchronize heavily.
 */
public class Publisher implements IPublisher {

	private static Logger _log = LoggerFactory.getLogger(Publisher.class);
	private ExecutorService executorService = null;
	private ArrayBlockingQueue<String> outputBuffer = new ArrayBlockingQueue<String>(
			1000);
	private ZMQ.Context context;
	private ZMQ.Socket envelopeSocket;
	private String topic;

	public Publisher(String topic) {
		this.topic = topic;
	}

	/**
	 * Bind ZeroMQ to the given host and port using the specified protocol.
	 * 
	 * @param protocol
	 *            "tcp" for example
	 * @param host
	 *            localhost, "*", or ip.
	 * @param port
	 *            port to bind to. Below 1024 requires elevated privs.
	 */
	public synchronized void open(String protocol, String host, int port) {
		context = ZMQ.context(1);
		// new envelope protocol
		envelopeSocket = context.socket(ZMQ.PUB);
		String bind = protocol + "://" + host + ":" + port;
		_log.warn("connecting to " + bind);
		/*
		 * do not bind to the socket, simply connect to existing socket provided by
		 * broker.
		 */
		envelopeSocket.connect(bind);
		executorService = Executors.newFixedThreadPool(1);
		executorService.execute(new SendThread(envelopeSocket, topic));

	}

	/**
	 * Ask ZeroMQ to close politely.
	 */
	public synchronized void close() {
		_log.warn("shutting down...");
		executorService.shutdownNow();
		try {
			Thread.sleep(1 * 1000);
		} catch (InterruptedException ie) {

		}
		envelopeSocket.close();
		context.term();
	}

	/**
	 * Publish a message to a topic. Be aware that fist message may be lost as
	 * subscriber will not connect in time.
	 * 
	 * @param message
	 *            the content of the message
	 */
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

		StringBuffer prefix = new StringBuffer();
		prefix.append("{\"RealtimeEnvelope\": {\"UUID\":\"")
				.append(generateUUID()).append("\",\"timeReceived\": ")
				.append(timeReceived).append(",")
				.append(removeLastBracket(realtime)).append("}}");
		return prefix.toString();
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
		private ZMQ.Socket zmqSocket = null;
		private byte[] topicName = null;

		public SendThread(ZMQ.Socket socket, String topicName) {
			zmqSocket = socket;
			this.topicName = topicName.getBytes();
		}

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String r = outputBuffer.poll(100, TimeUnit.MILLISECONDS);
					if (r == null) {
						Thread.yield();
						continue;
					}
					zmqSocket.send(topicName, ZMQ.SNDMORE);
					zmqSocket.send(r.getBytes(), 0);

				} catch (InterruptedException ie) {
					return;
				}

				Thread.yield();

				if (processedCount > 1000) {
					_log.warn("HTTP Proxy output queue: processed 1000 messages in "
							+ (new Date().getTime() - markTimestamp.getTime())
							/ 1000
							+ " seconds; current queue length is "
							+ outputBuffer.size());

					markTimestamp = new Date();
					processedCount = 0;
				}

				processedCount++;
			}
		}
	}
}
