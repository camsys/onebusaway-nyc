/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.capi;

import org.onebusaway.container.refresh.Refreshable;

import org.onebusaway.nyc.transit_data_federation.services.capi.CapiDao;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class CapiDaoHttpImpl implements CapiDao {

    private static Logger _log = LoggerFactory.getLogger(CapiDaoHttpImpl.class);

    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;

    public int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private String _url = null;

    private ConfigurationService _configurationService;

    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    public void setConfig(ConfigurationService config) {
        _configurationService = config;
    }

    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler scheduler) {
        _taskScheduler = scheduler;
    }

    public void setConnectionTimeout(int timeout) {
        _connectionTimeout = timeout;
    }

    @Override
    public void setLocation(String url){
        _url = url;
    }

    @Override
    public String getLocation() {
        return _url;
    }

    public ConfigurationService getConfig() {
        return _configurationService;
    }

    @PostConstruct
    public void setup() {
        _log.info("starting dao");
        refreshConfig();
        createConfigThread();
    }

    @Refreshable(dependsOn = {"tds.capiUrl", "tds.capiConnectionTimeout"})
    public void refreshConfig() {
        setLocation(getConfig().getConfigurationValueAsString("tds.capiUrl", null));
        setConnectionTimeout(getConfig().getConfigurationValueAsInteger("tds.capiConnectionTimeout", 1000));
        _log.debug("successfully refreshed configuration");
    }

    private void createConfigThread() {
        if (_taskScheduler != null) {
            ConfigThread configThread = new ConfigThread(this);
            _taskScheduler.scheduleWithFixedDelay(configThread, 60 * 1000);
            _log.info("config thread successfully created");
        }
    }

    @Override
    public InputStream getCancelledTripData(){
        HttpURLConnection connection;
        if (getLocation() == null) {
            _log.warn("Cancelled trips url not configured, exiting");
            return null;
        }
        try {
            connection = (HttpURLConnection) new URL(_url).openConnection();
            connection.setConnectTimeout(_connectionTimeout);
            connection.setReadTimeout(_connectionTimeout);
            return connection.getInputStream();
        } catch (Exception any) {
            _log.error("issue retrieving " + getLocation() + ", " + any.toString());
            return null;
        }
    }

    public static class ConfigThread implements Runnable {
        private CapiDaoHttpImpl resource;
        public ConfigThread(CapiDaoHttpImpl resource) {
            this.resource = resource;
        }
        @Override
        public void run() {
            resource.refreshConfig();
        }
    }
}
