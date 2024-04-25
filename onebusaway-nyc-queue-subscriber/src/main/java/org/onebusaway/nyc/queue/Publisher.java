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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.eaio.uuid.UUID;

import javax.annotation.PostConstruct;

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
	private String protocol;
	private String host;
	private int port;

	public Publisher(String topic) {
		this.topic = topic;
	}

	/**
	 * Bind ZeroMQ to the given host and port using the specified protocol.
	 ** @param topic
	 *            zeromq topic
	 * @param protocol
	 *            "tcp" for example
	 * @param host
	 *            localhost, "*", or ip.
	 * @param port
	 *            port to bind to. Below 1024 requires elevated privs.
	 */
	public Publisher(String topic, String protocol, String host, Integer port) {
		this.topic = topic;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
	}


	@PostConstruct
	@Override
	public synchronized void init() {
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
		envelopeSocket.close();
		context.term();
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
		private ZMQ.Socket zmqSocket = null;
		private byte[] topicName = null;

		private String topic;

		public SendThread(ZMQ.Socket socket, String topicName) {
			zmqSocket = socket;
			this.topic = topicName;
			this.topicName = topicName.getBytes();
		}

		public void run() {
			boolean error = false;
			int errorCount = 0;
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String r = outputBuffer.take();
					boolean success = zmqSocket.send(topicName, ZMQ.SNDMORE);
					if (success) {
						zmqSocket.send(r.getBytes(), 0);
					} else {
						error = true;
						errorCount++;
					}

				} catch (InterruptedException ie) {
					return;
				}

				if (processedCount > 1000) {
					_log.warn("HTTP Proxy output queue: processed 1000 messages in "
							+ (new Date().getTime() - markTimestamp.getTime())
							/ 1000
							+ " seconds; current queue length is "
							+ outputBuffer.size());

					if(error) {
						_log.info("Send error condition occured " +errorCount + " times" );
						errorCount = 0;
					}
					markTimestamp = new Date();
					processedCount = 0;
				}

				processedCount++;
			}
		}
	}
}
