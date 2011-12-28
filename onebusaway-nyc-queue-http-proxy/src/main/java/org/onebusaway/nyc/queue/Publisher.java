package org.onebusaway.nyc.queue;

import com.eaio.uuid.UUID;
import org.zeromq.ZMQ;

/**
 * Encapsulate ZeroMQ queue operations.  ZeroMQ prefers to be on its own
 * thread and not be hit simultaneously, so synchronize heavily.
 */
public class Publisher implements IPublisher {

    private ZMQ.Context context;
    private ZMQ.Socket envelopeSocket;
    private String topic;
    public Publisher(String topic) {
        this.topic = topic;
    }

    /**
     * Bind ZeroMQ to the given host and port using the specified protocol.
     * @param protocol "tcp" for example
     * @param host localhost, "*", or ip.
     * @param port port to bind to.  Below 1024 requires elevated privs.
     */
    public synchronized void open(String protocol, String host, int port) {
        context = ZMQ.context(1);
	// new envelope protocol
        envelopeSocket = context.socket(ZMQ.PUB);
        envelopeSocket.bind(protocol + "://" + host + ":" + (port+1));
    }

    /**
     * Ask ZeroMQ to close politely.
     */
    public synchronized void close() {
        envelopeSocket.close();
        context.term();
    }

    /**
     * Publish a message to a topic.  Be aware that fist message may be lost
     * as subscriber will not connect in time.
     * @param message the content of the message
     */
    public synchronized void send(byte[] message) {
        envelopeSocket.send(topic.getBytes(), ZMQ.SNDMORE);
        envelopeSocket.send(wrap(message).getBytes(), 0);
    }

    String wrap(byte[] message) {
				long timeReceived = getTimeReceived();
				String realtime = new String(message);
				// we remove wrapping below, so check for min length acceptable
				if (realtime.length() < 2) return null;
 

				StringBuffer prefix = new StringBuffer();
				prefix.append("{\"RealtimeEnvelope\": {\"UUID\":\"")
						.append(generateUUID())
						.append("\",\"timeReceived\": ")
						.append(timeReceived)
						.append(",")
  					.append(removeLastBracket(realtime))
						.append("}}");
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

}