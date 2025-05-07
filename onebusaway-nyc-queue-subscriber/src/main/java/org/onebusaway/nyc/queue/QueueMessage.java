package org.onebusaway.nyc.queue;

public class QueueMessage {
    public final String topic;
    public final byte[] payload;

    public QueueMessage(String topic, byte[] payload) {
        this.topic = topic;
        this.payload = payload;
    }

    public String getTopic() {
        return topic;
    }

    public byte[] getPayload() {
        return payload;
    }
}

