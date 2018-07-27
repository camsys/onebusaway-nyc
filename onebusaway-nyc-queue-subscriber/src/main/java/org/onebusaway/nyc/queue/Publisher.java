package org.onebusaway.nyc.queue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.eaio.uuid.UUID;

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
	private static final int TWO_MIN_MILLIS = 60*2*1000;

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

		return replaceInvalidRmcDateTime(prefix, timeReceived);
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

	String replaceInvalidRmcDateTime(StringBuffer realtime, long timeReceived){
		try {
			String[] rmcData = getRmcData(realtime);

			Date rmcDateTime = getRmcDateTime(rmcData);
			if(!isRmcDateValid(rmcDateTime)){
				Date timeReceivedDate = new Date(timeReceived);
				replaceRmcDate(rmcData, timeReceivedDate);
				if(!isRmcTimeValid(rmcDateTime, timeReceivedDate)){
					replaceRmcTime(rmcData, timeReceivedDate);
				}
				String rmcDataString = StringUtils.join(rmcData, ",");
				replaceRmcData(realtime, rmcDataString);
			}
		}catch (Exception e){
			_log.warn("Unable to replace invalid rmc date time", e);
		}
		return realtime.toString();
	}

	void replaceRmcData(StringBuffer realtime, String rmcDataString){
		int rmcIndex = realtime.lastIndexOf("$GPRMC");
		int endRmcIndex = realtime.indexOf("\"",rmcIndex);
		realtime.replace(rmcIndex, endRmcIndex, rmcDataString);
	}

	String [] getRmcData(StringBuffer realtime) throws StringIndexOutOfBoundsException{
		int rmcIndex = realtime.lastIndexOf("$GPRMC");
		int endRmcIndex = realtime.indexOf("\"",rmcIndex);
		return realtime.substring(rmcIndex, endRmcIndex).split(",");
	}

	Date getRmcDateTime(String[] rmcData) throws ParseException {
		String rmcDateTime = rmcData[9] + " " + rmcData[1];
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy hhmmss.S");
		Date date = sdf.parse(rmcDateTime);
		return date;
	}

	boolean isRmcDateValid(Date rmcDate){
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.WEEK_OF_YEAR, -1000);
		return cal.getTime().before(rmcDate);
	}

	boolean isRmcTimeValid(Date rmcDate, Date timeReceived){
		int rmcTime;
		int timeReceivedTime;

		timeReceivedTime = (int) (timeReceived.getTime() % (24*60*60*1000L));
		rmcTime = (int) (rmcDate.getTime() % (24*60*60*1000L));
		return (timeReceivedTime - rmcTime < (TWO_MIN_MILLIS));
	}

	void replaceRmcDate(String[] rmcData, Date timeReceived){
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
		rmcData[9] = sdf.format(timeReceived);
	}

	void replaceRmcTime(String[] rmcData, Date timeReceived){
		SimpleDateFormat sdf = new SimpleDateFormat("hhmmss.S");
		rmcData[1] = sdf.format(timeReceived);
	}

	private class SendThread implements Runnable {

		int processedCount = 0;
		Date markTimestamp = new Date();
		private ZMQ.Socket zmqSocket = null;
		private byte[] topicName = null;
		//private String topic;

		public SendThread(ZMQ.Socket socket, String topicName) {
			zmqSocket = socket;
			//topic = topicName;
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
