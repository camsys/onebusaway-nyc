package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.queue.HTTPListenerTask;
import org.onebusaway.nyc.util.impl.S3Utility;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
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
 *
 * reads in csv from s3 with list of route to type
 *
 * saves this information in a hashmap
 *
 * rechecks S3 regularly for updates
 *
 * when incoming to determine if route is express, compares against hashmap, returns true if express
 *
 *
 */
@Component
public class NycRouteTypeService {
    protected static Logger _log = LoggerFactory.getLogger(NycRouteTypeService.class);

    private Map<AgencyAndId, RouteType> _routesToNycType = new HashMap<AgencyAndId, RouteType>();

    private long _updateInterval = 10* 60 * 1000;

    Set<AgencyAndId> _expressRoutes;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;


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
        String s3Username = System.getProperty("s3.username");
        String s3Password = System.getProperty("s3.password");
        String path = System.getProperty("s3.suplimentalRouteTypesPath");
        S3Utility s3Utility = new S3Utility(s3Username,s3Password,S3Utility.getBucketFromS3Path(path));
        return s3Utility.get(S3Utility.getKeyFromS3Path(path));
    }

    // method to read in data from s3
    public void updateNycRouteTypeData(InputStream data) throws IOException {
        Map<AgencyAndId, RouteType> routesToNycType = new HashMap<AgencyAndId, RouteType>();

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
                    RouteType routeType = RouteType.fromString(typeValue);
                    routesToNycType.put(AgencyAndIdLibrary.convertFromString(routeId), routeType);
                }
            }
            _routesToNycType = routesToNycType;
            _expressRoutes = null;
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid CSV format: " + e.getMessage());
        }
    }



    public boolean isRouteExpress(AgencyAndId routeId) {
        if(!_routesToNycType.containsKey(routeId)) {
            return false;
        }
        return _routesToNycType.get(routeId).equals(RouteType.EXPRESS);
    }

    public Set<AgencyAndId> getExpressRoutes() {
        if(_expressRoutes==null) {
            _expressRoutes = new HashSet<AgencyAndId>();
            for (Map.Entry<AgencyAndId, RouteType> entry : _routesToNycType.entrySet()) {
                if (entry.getValue().equals(RouteType.EXPRESS)) {
                    _expressRoutes.add(entry.getKey());
                }
            }
        }
        return new HashSet<AgencyAndId>(_expressRoutes);
    }



    public enum RouteType {
        FEEDER,
        GRID,
        EXPRESS,
        UNIDENTIFIED;

        public static RouteType fromString(String value) {
            if (value == null || value.isBlank()) {
                return UNIDENTIFIED;
            }
            try {
                return RouteType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNIDENTIFIED;
            }
        }
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
