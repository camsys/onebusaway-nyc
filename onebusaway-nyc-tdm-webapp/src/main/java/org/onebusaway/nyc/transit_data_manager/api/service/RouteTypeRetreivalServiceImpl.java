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
package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RouteType;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.DataRetreivalService;
import org.onebusaway.nyc.util.impl.HttpDataRetreivalService;
import org.onebusaway.nyc.util.impl.S3DataRetreivalService;
import org.onebusaway.nyc.util.impl.S3Utility;
import org.onebusaway.nyc.util.property.PropertyUtil;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Component
public class RouteTypeRetreivalServiceImpl implements RouteTypeRetreivalService {

    protected static Logger _log = LoggerFactory.getLogger(RouteTypeRetreivalServiceImpl.class);
    private volatile Map<AgencyAndId, RouteType> _routesToNycType = new HashMap<>();

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    private ConfigurationService _configurationService;

    private DataRetreivalService dataRetreivalService;

    private static final String TYPE_COLUMN_IDENTIFIER = "type";
    private static final String ROUTE_ID_IDENTIFIER = "route_id";
    private ScheduledFuture<?> _updateTask = null;

    private boolean _enabled;
    private String _dataPath;

    @PreDestroy
    public void destroy() {
        _log.info("destroy");
        if (_taskScheduler != null) {
            _taskScheduler.shutdown();
        }
    }

    @PostConstruct
    public void setup(){
        configChanged();
    }

    @Refreshable(dependsOn = {"tdm.routeTypeRefreshInterval", "tdm.routeTypeServiceEnabled", "tdm.routeTypeUrl"})
    private void configChanged() {
        _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("tdm.routeTypeServiceEnabled", "false"));
        if(_enabled) {
            initializeDataRetreivalService();
            Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.routeTypeRefreshInterval", 60);
            if (updateIntervalSecs != null && dataRetreivalService != null) {
                setUpdateFrequency(updateIntervalSecs);
            } else {
                cancelTask();
            }
        }
    }

    public void setDataRetreivalService(DataRetreivalService dataRetreivalService) {
        this.dataRetreivalService = dataRetreivalService;
    }

    private void initializeDataRetreivalService()  {
        _dataPath = _configurationService.getConfigurationValueAsString("tdm.routeTypeUrl", null);
        try {
            if(_dataPath == null || _dataPath.isBlank()){
                return;
            }
            if(S3Utility.isS3Path(_dataPath)){
                initializeS3DataRetreivalService(_dataPath);
            } else {
                initializeHttpDataRetreivalService();
            }
        } catch (Exception e){
            _log.error("Unable to instantiate Data Retreival Service from {}", _dataPath);
        }
    }

    private void initializeHttpDataRetreivalService() {
        setDataRetreivalService(new HttpDataRetreivalService());
    }

    private void initializeS3DataRetreivalService(String path) throws Exception {
        String s3Username = PropertyUtil.getProperty("S3_ROUTETYPE_USER");
        String s3Password = PropertyUtil.getProperty("S3_ROUTETYPE_PASSWORD");
        if(s3Username != null && s3Password != null && path != null){
           setDataRetreivalService(new S3DataRetreivalService(s3Username, s3Password, path));
        }
    }

    private void cancelTask(){
        if(_updateTask != null){
            _updateTask.cancel(true);
        }
    }

    private void setUpdateFrequency(int seconds) {
        cancelTask();
        if(_enabled){
            _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
        }
    }


    private class UpdateThread extends TimerTask {
        @Override
        public void run() {
            try {
                InputStream inputStream = dataRetreivalService.get(_dataPath);
                updateRouteTypeData(inputStream);
            } catch (Exception e) {
                _log.error("refreshData() failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void updateRouteTypeData(InputStream inputStream) throws IOException {
        Map<AgencyAndId, RouteType> routesToNycType = new HashMap<>();

        // find which collumn is the route id, and which is the type then read it into the hashmap
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> headers = Arrays.asList(reader.readLine().split(","));

            int routeIdIndex = headers.indexOf(ROUTE_ID_IDENTIFIER);
            int typeIndex = headers.indexOf(TYPE_COLUMN_IDENTIFIER);


            if (routeIdIndex == -1 || typeIndex == -1) {
                throw new IllegalArgumentException("CSV file missing required columns: " + ROUTE_ID_IDENTIFIER + ", " + TYPE_COLUMN_IDENTIFIER);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1); // Keep empty values
                if (values.length <= Math.max(routeIdIndex, typeIndex)) continue; // Skip malformed lines

                String routeId = values[routeIdIndex].trim();
                String typeValue = values[typeIndex].trim();

                if (!routeId.isEmpty()) {
                    RouteType routeType = RouteType.fromString(typeValue);
                    routesToNycType.put(AgencyAndIdLibrary.convertFromString(routeId), routeType);
                }
            }
            _routesToNycType = routesToNycType;
        } catch (IOException e) {
            _log.info("Error reading CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            _log.info("Invalid CSV format: " + e.getMessage());
        } catch (NullPointerException npe){
            _routesToNycType = Collections.EMPTY_MAP;
        }
    }

    @Override
    public Map<AgencyAndId, RouteType> getRouteTypes(){
       return _routesToNycType;
    }
}
