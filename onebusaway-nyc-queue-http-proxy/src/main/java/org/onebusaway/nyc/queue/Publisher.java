package org.onebusaway.nyc.queue;

import org.zeromq.ZMQ;

/**
 * Encapsulate ZeroMQ queue operations.  ZeroMQ prefers to be on its own
 * thread and not be hit simultaneously, so synchronize heavily.
 */
public class Publisher implements IPublisher {

    private ZMQ.Context context;
    private ZMQ.Socket socket;
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
        socket = context.socket(ZMQ.PUB);
        socket.bind(protocol + "://" + host + ":" + port);
    }

    /**
     * Ask ZeroMQ to close politely.
     */
    public synchronized void close() {
        socket.close();
        context.term();
    }

    /**
     * Publish a message to a topic.  Be aware that fist message may be lost
     * as subscriber will not connect in time.
     * @param message the content of the message
     */
    public synchronized void send(byte[] message) {
        socket.send(topic.getBytes(), ZMQ.SNDMORE);
        socket.send(message, 0);
    }

}