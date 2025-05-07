package org.onebusaway.nyc.queue;

public interface MessageQueueProvider {
    void initialize(String host, String queueName, Integer port) throws InterruptedException;
    QueueMessage nextMessage() throws InterruptedException;
    void shutdown();
}
