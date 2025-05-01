/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

