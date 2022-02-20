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

package org.onebusaway.nyc.transit_data_manager.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.api.IncomingNycCancelledTripBeansContainer;
import org.onebusaway.nyc.transit_data_manager.api.dao.CapiDao;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class CapiRetrievalServiceImpl implements CapiRetrievalService {

    private static Logger _log = LoggerFactory.getLogger(CapiRetrievalServiceImpl.class);

    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;

    private ThreadPoolTaskScheduler _taskScheduler;

    private List<NycCancelledTripBean> _cancelledTripBeans;

    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler scheduler) {
        _taskScheduler = scheduler;
    }

    private CapiDao _capiDao;

    @Autowired
    public void setCapiDao(CapiDao capiDao) {
        _capiDao = capiDao;
    }

    private ConfigurationService _configurationService;

    public ConfigurationService getConfig() {
        return _configurationService;
    }

    @Autowired
    public void setConfig(ConfigurationService config) {
        _configurationService = config;
    }

    private Integer _refreshInterval;

    private ObjectReader _objectReader = new ObjectMapper()
                                            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                                            .setTimeZone(Calendar.getInstance().getTimeZone())
                                            .readerFor(IncomingNycCancelledTripBeansContainer.class);

    private UpdateThread updateThread;

    private Boolean _isEnabled;

    @PostConstruct
    public void setup(){
        refreshConfig();
        createConfigThread();
        createUpdateThread();
    }

    @Refreshable(dependsOn = {"tdm.capiRefreshInterval","tdm.enableCapi"})
    protected void refreshConfig() {
        _isEnabled = getConfig().getConfigurationValueAsBoolean( "tdm.enableCapi", Boolean.FALSE);
        _refreshInterval = getConfig().getConfigurationValueAsInteger( "tdm.capiRefreshInterval", DEFAULT_REFRESH_INTERVAL);
    }

    protected void refreshUpdateThreadPostConfig(){
        // Only creates if does not already exist
        createUpdateThread();
    }

    private void createConfigThread(){
        if(_taskScheduler != null) {
            ConfigThread configThread = new ConfigThread(this);
            _taskScheduler.scheduleWithFixedDelay(configThread, 60 * 1000);
        } else {
            _log.warn("Unable to create config thread, task scheduler unavailable");
        }
    }

    private void createUpdateThread(){
        if (updateThread == null && _taskScheduler != null && allowCapi()) {
            updateThread = new UpdateThread(this);
            _log.info("configuring update thread now");
            _taskScheduler.scheduleWithFixedDelay(updateThread, _refreshInterval);
        }
    }

    private boolean allowCapi() {
        if (!isEnabled()){
            _log.warn("capi is not enabled in configuration");
            return false;
        }
        if (getLocation() == null){
            _log.warn("capi url is not defined");
            return false;
        }
        return true;
    }

    public Boolean isEnabled() {
        return _isEnabled;
    }

    @Override
    public String getLocation() {
        return _capiDao.getLocation();
    }

    @Override
    public List<NycCancelledTripBean> getCancelledTripBeans(){
        return _cancelledTripBeans;
    }

    @Override
    public void setCancelledTripBeans(List<NycCancelledTripBean> cancelledTripBeans){
        _cancelledTripBeans = cancelledTripBeans;
    }

    @Override
    public void updateCancelledTripBeans(){
        InputStream inputStream = getCancelledTripData();
        List<NycCancelledTripBean> cancelledTripBeans = convertCapiInputToCancelledTripBeans(inputStream);
        _cancelledTripBeans = cancelledTripBeans;
    }

    private InputStream getCancelledTripData(){
        return _capiDao.getCancelledTripData();
    }

    private List<NycCancelledTripBean> convertCapiInputToCancelledTripBeans(InputStream input) {
        _log.debug("reading from stream...");
        try {
            IncomingNycCancelledTripBeansContainer beansContainer = _objectReader.readValue(input, IncomingNycCancelledTripBeansContainer.class);
            if (beansContainer != null && beansContainer.getBeans() != null) {
                _log.debug("parsed " + beansContainer.getBeans().size() + " records");
            } else {
                _log.debug("empty beanContainer");
            }
            return beansContainer.getBeans();
        } catch (Exception any) {
            _log.error("issue parsing json: " + any, any);
        }
        return Collections.EMPTY_LIST;
    }

    public static class UpdateThread implements Runnable {

        private CapiRetrievalServiceImpl resource;

        public UpdateThread(CapiRetrievalServiceImpl resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            if (resource.allowCapi()) {
                _log.debug("refreshing...");
                resource.updateCancelledTripBeans();
                _log.debug("refresh complete");
            } else {
                _log.debug("disabled refresh");
                resource.setCancelledTripBeans(Collections.EMPTY_LIST);
            }
        }
    }

    public static class ConfigThread implements Runnable {
        private CapiRetrievalServiceImpl resource;
        public ConfigThread(CapiRetrievalServiceImpl resource) {
            this.resource = resource;
        }
        @Override
        public void run() {
            resource.refreshConfig();
            resource.refreshUpdateThreadPostConfig();
        }
    }

}