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

package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripPersistenceService;
import org.onebusaway.nyc.report_archive.services.CancelledTripRecordValidationService;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CancelledTripListenerTask {

    private static Logger _log = LoggerFactory.getLogger(CancelledTripListenerTask.class);

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    private NycTransitDataService _nycTransitDataService;

    @Autowired
    private ConfigurationService _configurationService;

    @Autowired
    private CancelledTripPersistenceService _persistor;

    @Autowired
    private CancelledTripRecordValidationService _validationService;

    private Boolean isEnabled;

    private Integer capiRefreshInterval;

    @PostConstruct
    public void setup() {

        ConfigThread configThread = new ConfigThread(this);
        _taskScheduler.scheduleWithFixedDelay(configThread, 2 * 1000);

        CancelledTripListenerThread thread = new CancelledTripListenerThread(_nycTransitDataService, _persistor,
                                                _validationService);

        _taskScheduler.scheduleWithFixedDelay(thread, TimeUnit.SECONDS.toMillis(getCapiRefreshInterval()));
    }

    private boolean allowCapi() {
        if (!isEnabled()){
            _log.warn("capi is not enabled in configuration");
            return false;
        }
        return true;
    }

    @Refreshable(dependsOn = {"archiver.enableCapi","archive.capiRefreshIntervalSec"})
    protected void refreshConfig() {
        isEnabled = _configurationService.getConfigurationValueAsBoolean("archive.enableCapi", false);
        capiRefreshInterval = _configurationService.getConfigurationValueAsInteger("archive.capiRefreshIntervalSec", 30);
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public Integer getCapiRefreshInterval() {
        return capiRefreshInterval;
    }

    public class CancelledTripListenerThread extends Thread {
        private NycTransitDataService tds;
        private CancelledTripPersistenceService persistor;
        private CancelledTripRecordValidationService validationService;

        public CancelledTripListenerThread(NycTransitDataService tds, CancelledTripPersistenceService persistor,
                                           CancelledTripRecordValidationService validationService) {
            this.tds = tds;
            this.persistor = persistor;
            this.validationService = validationService;
        }

        public void run() {
            if(!allowCapi()){
                return;
            }
            List<NycCancelledTripBean> cancelledTrips = tds.getAllCancelledTrips();
            processCancelledTrips(cancelledTrips);
        }


        public boolean processCancelledTrips(List<NycCancelledTripBean> cancelledTrips) {
            Map<String, NycCancelledTripRecord> recordSet = new HashMap<>();
            NycCancelledTripRecord record = null;
            try {
                for(NycCancelledTripBean cancelledTrip : cancelledTrips){
                    record = new NycCancelledTripRecord(cancelledTrip);
                    boolean isValid = validationService.isValidRecord(record);
                    if(isValid){
                        recordSet.put(record.getTrip(), record);
                    }
                }
                persistor.persistAll(recordSet);

            } catch (Throwable t) {
                _log.error("Exception processing contents {}",record, t);
                return false;
            }

            return true;
        }

    }

    public static class ConfigThread implements Runnable {
        private CancelledTripListenerTask _task;
        public ConfigThread(CancelledTripListenerTask task) {
            _task = task;
        }

        @Override
        public void run() {
            _task.refreshConfig();
        }
    }

}
