package org.onebusaway.nyc.transit_data_federation.impl.queue.interfaces;

import org.onebusaway.nyc.queue.QueueListenerTask;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public interface InferenceQueueListenerInterface {

    @Autowired
    ThreadPoolTaskScheduler _taskScheduler = new ThreadPoolTaskScheduler();

    boolean processMessage(String address, byte[] buff);
    String getQueueHost();

    public String getQueueName();

    Integer getQueuePort();

    void setup();
    void destroy();

    void startListenerThread();
}

