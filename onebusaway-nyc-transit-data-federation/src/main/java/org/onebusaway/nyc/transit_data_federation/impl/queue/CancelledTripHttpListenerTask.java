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

package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpResponse;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.capi.CancelledTripService;
import org.onebusaway.nyc.transit_data_federation.services.capi.CapiDao;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CancelledTripHttpListenerTask {

    protected static Logger _log = LoggerFactory.getLogger(CancelledTripHttpListenerTask.class);

    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;

    private ThreadPoolTaskScheduler _taskScheduler;

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

    private NycTransitDataService _tds;

    @Autowired
    public void setNycTransitDataService(NycTransitDataService tds){
        _tds = tds;
    }

    private CancelledTripService _cancelledTripService;

    @Autowired
    public void setCancelledTripService(CancelledTripService cancelledTripService){
        _cancelledTripService = cancelledTripService;
    }


    private Integer _refreshInterval;

    private ObjectMapper _objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setTimeZone(Calendar.getInstance().getTimeZone());

    private UpdateThread updateThread;

    private Boolean _isEnabled;

    @PostConstruct
    public void setup(){
        refreshConfig();
        createConfigThread();
        createUpdateThread();
    }


    @Refreshable(dependsOn = {"tds.enableCapi","tds.capiRefreshInterval"})
    protected void refreshConfig() {
        _refreshInterval = getConfig().getConfigurationValueAsInteger( "tds.capiRefreshInterval", DEFAULT_REFRESH_INTERVAL);
        _isEnabled =  getConfig().getConfigurationValueAsBoolean("tds.enableCapi", Boolean.FALSE);
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
        if (allowCapi() && updateThread == null && _taskScheduler != null) {
            updateThread = new UpdateThread(this);
            _log.info("configuring update thread now");
            _taskScheduler.scheduleWithFixedDelay(updateThread, _refreshInterval);
        }
    }

    protected void refreshUpdateThreadPostConfig(){
        // Only creates if does not already exist
        createUpdateThread();
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

    public String getLocation() {
        return _capiDao.getLocation();
    }


    public void updateCancelledTripBeans()  {
        try {
            InputStream inputStream = getCancelledTripData();
            List<NycCancelledTripBean> cancelledTripBeans = convertResponseToCancelledTripBeans(inputStream);
            Map<AgencyAndId, NycCancelledTripBean> cancelledTripsMap = convertCancelledTripsToMap(cancelledTripBeans);
            _cancelledTripService.updateCancelledTrips(cancelledTripsMap);
        } catch (Exception e){
            _log.error("Unable to process cancelled trip beans from {}", getLocation());
        }
    }

    private InputStream getCancelledTripData(){
        return _capiDao.getCancelledTripData();
    }

    private List<NycCancelledTripBean> convertResponseToCancelledTripBeans(InputStream input) {
        try {
            List<NycCancelledTripBean> list = _objectMapper.readValue(input,
                    new TypeReference<List<NycCancelledTripBean>>() {});
            return list;
        } catch (Exception any) {
            _log.error("issue parsing json: " + any, any);
        }
        return Collections.EMPTY_LIST;
    }


    private Map<AgencyAndId, NycCancelledTripBean> convertCancelledTripsToMap(List<NycCancelledTripBean> cancelledTripBeans) {
        Map<AgencyAndId, NycCancelledTripBean> results = new ConcurrentHashMap<>();
        for (NycCancelledTripBean cancelledTrip : cancelledTripBeans) {
            if (isValidCancelledTrip(cancelledTrip)) {
                results.put(AgencyAndId.convertFromString(cancelledTrip.getTrip()), cancelledTrip);
            }
        }
        return results;
    }

    private boolean isValidCancelledTrip(NycCancelledTripBean cancelledTripBean) {
        try {
            if ((cancelledTripBean.getStatus().equalsIgnoreCase("canceled") ||
                    cancelledTripBean.getStatus().equalsIgnoreCase("cancelled"))) {
                if (_tds.getTrip(cancelledTripBean.getTrip()) == null) {
                    _log.debug("unknown tripId=" + cancelledTripBean.getTrip());
                    return false;
                } else {
                    return true;
                }
            }
        } catch (ServiceException e) {
            _log.warn("Error retrieving cancelled trip", e);
        }
        return false;
    }

    public static class UpdateThread implements Runnable {

        private CancelledTripHttpListenerTask resource;

        public UpdateThread(CancelledTripHttpListenerTask resource) {
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
            }
        }
    }

    public static class ConfigThread implements Runnable {
        private CancelledTripHttpListenerTask resource;
        public ConfigThread(CancelledTripHttpListenerTask resource) {
            this.resource = resource;
        }
        @Override
        public void run() {
            resource.refreshConfig();
            resource.refreshUpdateThreadPostConfig();
        }
    }
}
