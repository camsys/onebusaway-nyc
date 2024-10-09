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
