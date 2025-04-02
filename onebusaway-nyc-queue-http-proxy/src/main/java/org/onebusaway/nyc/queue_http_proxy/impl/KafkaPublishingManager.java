package org.onebusaway.nyc.queue_http_proxy.impl;

import com.fasterxml.jackson.databind.JsonNode;

public interface KafkaPublishingManager {

    void sendMessage(JsonNode jsonMessage);

    void close();


}
