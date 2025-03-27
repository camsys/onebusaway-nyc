/**
 * Copyright (C) 2025 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_federation.impl.nyc;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RouteType;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.DataRetreivalService;
import org.onebusaway.nyc.util.impl.HttpDataRetreivalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for determining type of route, currently exclusively if route is express or not.
 */
@Component
public class NycRouteTypeService {
    protected static Logger _log = LoggerFactory.getLogger(NycRouteTypeService.class);
    private volatile Map<AgencyAndId, RouteType> _routesToNycType = new HashMap<>();

    private volatile Set<AgencyAndId> _expressRoutes = new HashSet<>();
    private DataRetreivalService dataRetreivalService;
    private ScheduledFuture<?> _updateTask = null;
    private ObjectReader objectReader;

    private String _dataPath;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    private ConfigurationService _configurationService;

    public NycRouteTypeService(){
        SimpleModule agencyAndIdDeserializer = new SimpleModule();
        agencyAndIdDeserializer.addKeyDeserializer(AgencyAndId.class, new AgencyAndIdDeserializer());

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JaxbAnnotationModule())
                .registerModule(agencyAndIdDeserializer);
        objectReader = mapper.readerFor(new TypeReference<Map<AgencyAndId, RouteType>>() {});
    }

    @PostConstruct
    public void setup() throws IOException {
        configRefresh();
    }

    @Refreshable(dependsOn = {"tds.routeTypeRefreshInterval", "tds.routeTypeUrl"})
    public void configRefresh() {
        initializeDataRetreivalService();
        Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.routeTypeRefreshInterval", 60);
        if (updateIntervalSecs != null && dataRetreivalService != null) {
            cancelUpdateTask();
            createUpdateTask(updateIntervalSecs);
        } else {
            _log.warn("Unable to create thread to regularly update nycRouteType, task scheduler unavailable");
            cancelUpdateTask();
        }
    }

    private void createUpdateTask(Integer updateIntervalSecs) {
        _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), updateIntervalSecs * 1000);
    }

    private void cancelUpdateTask(){
        if(_updateTask != null){
            _updateTask.cancel(true);
        }
    }

    private void initializeDataRetreivalService() {
        _dataPath = _configurationService.getConfigurationValueAsString("tds.routeTypeUrl", null);
        try {
            if(_dataPath == null){
                return;
            }
            initializeHttpDataRetreivalService();

        } catch (Exception e){
            _log.error("Unable to instantiate Data Retreival Service from {}", _dataPath);
        }
    }

    private void initializeHttpDataRetreivalService() {
        dataRetreivalService = new HttpDataRetreivalService();
    }


    private class UpdateThread implements Runnable {
        @Override
        public void run() {
            try {
                InputStream inputStream = dataRetreivalService.get(_dataPath);
                updateNycRouteTypeData(inputStream);
            } catch (Exception e) {
                _log.error("refreshData() failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void updateNycRouteTypeData(InputStream inputStream) {
        Map<AgencyAndId, RouteType> routesToNycType;
        Set<AgencyAndId> expressRoutes;
        try{
            routesToNycType = objectReader.readValue(inputStream);
            expressRoutes = populateExpressRoutes(routesToNycType);

            _routesToNycType = routesToNycType;
            _expressRoutes = expressRoutes;
        } catch (Exception e){
            _log.error("Unable to retrieve list of route types from url", e);
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    _log.warn("Unable to close inputStream");
                }
            }
        }
    }

    private Set<AgencyAndId> populateExpressRoutes(Map<AgencyAndId, RouteType> routesToNycType) {
        Set<AgencyAndId> expressRoutes = new HashSet<>();
        for (Map.Entry<AgencyAndId, RouteType> entry : routesToNycType.entrySet()) {
            if (entry.getValue().equals(RouteType.EXPRESS)) {
                expressRoutes.add(entry.getKey());
            }
        }
        return expressRoutes;
    }

    public Map<AgencyAndId, RouteType> getRouteTypes(){
        return _routesToNycType;
    }

    public Set<AgencyAndId> getExpressRoutes(){
        return _expressRoutes;
    }


    public boolean isRouteExpress(AgencyAndId routeId) {
        return _expressRoutes.contains(routeId);
    }

}
