package org.onebusaway.nyc.queue_http_proxy;

/**
 * Represents an interface to simply message queue publishing operations.
 * This is not attempting to be JMS.  This is merely hiding the details of
 * ZeroMQ for easier testing.
 */
public interface IPublisher {

    public void open(String protocol, String host, int port);
    public void close();
    public void send(byte[] message);
}