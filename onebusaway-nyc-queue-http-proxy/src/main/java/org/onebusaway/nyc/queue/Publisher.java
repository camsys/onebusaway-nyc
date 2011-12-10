package org.onebusaway.nyc.queue;

import com.eaio.uuid.UUID;
import org.zeromq.ZMQ;

/**
 * Encapsulate ZeroMQ queue operations.  ZeroMQ prefers to be on its own
 * thread and not be hit simultaneously, so synchronize heavily.
 */
public class Publisher implements IPublisher {

    private ZMQ.Context context;
    private ZMQ.Socket legacySocket;
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
	// for old protocol
        legacySocket = context.socket(ZMQ.PUB);
        legacySocket.bind(protocol + "://" + host + ":" + port);
	// new envelope protocol
        envelopeSocket = context.socket(ZMQ.PUB);
        envelopeSocket.bind(protocol + "://" + host + ":" + (port+1));
    }

    /**
     * Ask ZeroMQ to close politely.
     */
    public synchronized void close() {
        legacySocket.close();
        envelopeSocket.close();
        context.term();
    }

    /**
     * Publish a message to a topic.  Be aware that fist message may be lost
     * as subscriber will not connect in time.
     * @param message the content of the message
     */
    public synchronized void send(byte[] message) {
        legacySocket.send(topic.getBytes(), ZMQ.SNDMORE);
        legacySocket.send(message, 0);

        envelopeSocket.send(topic.getBytes(), ZMQ.SNDMORE);
        envelopeSocket.send(wrap(message).getBytes(), 0);
    }

    String wrap(byte[] message) {
	long timeReceived = System.currentTimeMillis();
	String realtime = new String(message);
	String uuid = new UUID().toString();
	StringBuffer prefix = new StringBuffer();
	prefix.append("{\"UUID\":\"")
	    .append(uuid)
	    .append("\",\"timeReceived\":")
	    .append(timeReceived)
	    .append(",\"ccLocationReport\":")
	    .append(realtime)
	    .append("}");
	return prefix.toString();
    }

}