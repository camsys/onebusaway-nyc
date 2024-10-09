package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.springframework.beans.factory.annotation.Value;

public class AnalysisOutputQueueSenderServiceImpl extends OutputQueueSenderServiceImpl {

    @Value("${inference-engine.outputQueueHost}")
    private String inputQueueHost;

    @Value("${inference-engine.outputQueueName}")
    private String inputQueueName;

    @Value("${inference-engine.outputQueuePort}")
    private Integer inputQueuePort;

    @Override
    public String getQueueHost() {
        return inputQueueHost;
    }

    @Override
    public String getQueueName() {
        return inputQueueName;
    }

    @Override
    public Integer getQueuePort() {
        return inputQueuePort;
    }
}
