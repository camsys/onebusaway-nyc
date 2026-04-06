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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.SupplementalRouteType;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.S3Utility;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

/**
 * Service for determining type of route, currently exclusively if route is express or not.
 */
@Component
public class NycRouteTypeService {
    protected static Logger _log = LoggerFactory.getLogger(NycRouteTypeService.class);

    private Map<AgencyAndId, SupplementalRouteType> _routesToNycType = new HashMap<AgencyAndId, SupplementalRouteType>();

    private long _updateInterval = 10* 60 * 1000;

    Set<AgencyAndId> _expressRoutes;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    private ConfigurationService _configurationService;


    @PostConstruct
    public void setup() throws IOException {
        if(_taskScheduler != null) {
            UpdateThread updateThread = new UpdateThread(this);
            _taskScheduler.scheduleWithFixedDelay(updateThread, _updateInterval);
        } else {
            _log.warn("Unable to create thread to regularly update nycRouteType, task scheduler unavailable");
            updateNycRouteTypeData(getDataFromS3());
        }
    }

    String TYPE_COLUMN_IDENTIFIER = "type";
    String ROUTE_ID_IDENTIFIER = "route_id";

    public NycRouteTypeService(){
    }

    public NycRouteTypeService(InputStream data) throws IOException {
        updateNycRouteTypeData(data);
    }

    // method to pull in the csv from s3
    public InputStream getDataFromS3() {
        String s3Username = System.getProperty("s3.user");
        String s3Password = System.getProperty("s3.password");
        String path = _configurationService.getConfigurationValueAsString("tdm.supplementalRouteTypesPath", null);
        S3Utility s3Utility = new S3Utility(s3Username,s3Password,S3Utility.getBucketFromS3Path(path));
        InputStream data = s3Utility.get(S3Utility.getKeyFromS3Path(path));
        _log.info("Retrieved supplemental route type data from s3 from "+ path);
        return data;
    }

    // method to read in data from s3
    public void updateNycRouteTypeData(InputStream data) throws IOException {
        Map<AgencyAndId, SupplementalRouteType> routesToNycType = new HashMap<AgencyAndId, SupplementalRouteType>();

        // find which collumn is the route id, and which is the type then read it into the hashmap
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(data))) {
            List<String> headers = Arrays.asList(reader.readLine().split(","));

            int routeIdIndex = headers.indexOf("route_id");
            int typeIndex = headers.indexOf("type");


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
                    SupplementalRouteType routeType = SupplementalRouteType.fromString(typeValue);
                    if(routeType == SupplementalRouteType.UNIDENTIFIED || routeType == null) {
                        _log.warn("Route type " + typeValue + " not recognized for route ID " + routeId);
                    }
                    routesToNycType.put(AgencyAndIdLibrary.convertFromString(routeId), routeType);
                }
            }
            _routesToNycType = routesToNycType;
            _expressRoutes = null;
        } catch (IOException e) {
            _log.error("Error reading CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            _log.error("Invalid CSV format: " + e.getMessage());
        }
        if(_routesToNycType.isEmpty()) {
            _log.error("No route types found in nycRouteType data");
        } else {
            _log.info("Loaded " + _routesToNycType.size() + " route types from nycRouteType data");
        }
    }



    public SupplementalRouteType getRouteType(AgencyAndId routeId) {
        if(!_routesToNycType.containsKey(routeId)) {
//            if(routeId!=null){_log.warn("Assessing route type: Route ID " + routeId + " not found in route type data.");}
            return SupplementalRouteType.UNIDENTIFIED;
        }
        return _routesToNycType.get(routeId);
    }

    public boolean isRouteExpress(AgencyAndId routeId) {
        if(routeId==null) {
            return false;
        }
        if(!_routesToNycType.containsKey(routeId)) {
            _log.warn("Assessing express status: Route ID " + routeId + " not found in route type data.");
            return false;
        }
        return _routesToNycType.get(routeId).equals(SupplementalRouteType.EXPRESS);
    }

    public Set<AgencyAndId> getExpressRoutes() {
        if(_expressRoutes==null) {
            _expressRoutes = new HashSet<AgencyAndId>();
            for (Map.Entry<AgencyAndId, SupplementalRouteType> entry : _routesToNycType.entrySet()) {
                if (entry.getValue().equals(SupplementalRouteType.EXPRESS)) {
                    _expressRoutes.add(entry.getKey());
                }
            }
        }
        return new HashSet<AgencyAndId>(_expressRoutes);
    }


    public static class UpdateThread implements Runnable {

        private NycRouteTypeService resource;

        public UpdateThread(NycRouteTypeService resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            try {
                resource.updateNycRouteTypeData(resource.getDataFromS3());
            } catch (IOException e) {
                _log.error("Error updating nyc supplemental route types: " + e.getMessage());
            }
        }
    }

}
