/**
 * Copyright (C) 2017 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.QueueListenerTask;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for attaching to APC Queue.
 */
public abstract class ApcQueueListenerTask extends QueueListenerTask {

    public enum Status {
        ENABLED, // read from queue
        TESTING, // don't read from queue, but allow cache to be read.
        DISABLED; // totally disabled
    };

    private Status status = Status.ENABLED;

    public void setStatus(Status status) { this.status = status; }

    protected static Logger _log = LoggerFactory
            .getLogger(ApcQueueListenerTask.class);


    protected abstract void processResult(NycVehicleLoadBean message, String contents);


    public Boolean useApcIfAvailable() {
        if (!Status.ENABLED.equals(status)) return false;
        return _configurationService.getConfigurationValueAsBoolean("tds.useApc", Boolean.FALSE);
    }


    @Override
    public boolean processMessage(String contents, byte[] buff) throws Exception {        

        if(StringUtils.isBlank(contents)){
          _log.warn("rejected message, message is empty");
          return false;
        }   

        if(!getQueueName().endsWith(contents)) {
            _log.warn("rejected message for queue " + contents);
        }

        try {
            NycVehicleLoadBean bean = _mapper.readValue(new String(buff), NycVehicleLoadBean.class);
            processResult(bean, new String(buff));
            return true;
        } catch (Exception any) {
            _log.warn("received corrupted APC message from queue; discarding: " + any.getMessage(), any);
            _log.warn("Contents=|" + contents + "|, buff=|" + new String(buff) + "|");
            return false;
        }
    }

    @Override
    @Refreshable(dependsOn = { "tds.apcQueueHost", "tds.apcQueuePort", "tds.apcQueueName", "tds.useApc" })
    public void startListenerThread() {
        if (_initialized == true) {
            _log.warn("Configuration service tried to reconfigure apc input queue reader; this service is not reconfigurable once started.");
            return;
        }

        if (!useApcIfAvailable()) {
            _log.error("apc integration disabled -- exiting");
            return;
        }

        String host = getQueueHost();
        String queueName = getQueueName();
        Integer port = getQueuePort();

        if (host == null) {
            _log.info("apc input queue is not attached; input hostname was not available via configuration service.");
            return;
        }

        _log.info("apc input queue listening on " + host + ":" + port + ", queue=" + queueName);

        try {
            initializeQueue(host, queueName, port);
        } catch (InterruptedException ie) {
            return;
        }

        _initialized = true;
    }

    @Override
    public String getQueueHost() {
        return _configurationService.getConfigurationValueAsString("tds.apcQueueHost", null);
    }

    @Override
    public String getQueueName() {
        return _configurationService.getConfigurationValueAsString("tds.apcQueueName", "apc");
    }

    @Override
    public String getQueueDisplayName() {
        return "apc";
    }

    @Override
    public Integer getQueuePort() {
        return _configurationService.getConfigurationValueAsInteger("tds.apcQueueOutputPort", 5576);
    }

    @Override
    public void startDNSCheckThread() {
        if (!useApcIfAvailable()) {
            _log.error("apc integration disabled; DNS check exiting");
            return;
        }
        _log.info("starting DNS check for APC queue " + getQueueName());
        super.startDNSCheckThread();
    }
}
