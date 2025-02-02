package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.onebusaway.nyc.util.impl.S3Utility;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
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
public class NycRouteTypeService {

    private static final String EXPRESS_ROUTES_FILE = "express_routes.csv";

    private Map<String, RouteType> _routesToNycType = new HashMap<String, RouteType>();

    private long _updateInterval = 60 * 1000;

    private AmazonS3 _s3 = new AmazonS3Client();

    @Autowired
    ConfigurationServiceImpl _configurationService;

    String TYPE_COLUMN_IDENTIFIER = "type";
    String ROUTE_ID_IDENTIFIER = "route_id";

    public NycRouteTypeService() throws IOException {
        updateExpressRoutes(getDataFromS3());
    }

    public NycRouteTypeService(InputStream data) throws IOException {
        updateExpressRoutes(data);
    }

    // method to pull in the csv from s3
    private InputStream getDataFromS3() {
        String s3Username = System.getProperty("s3.username");
        String s3Password = System.getProperty("s3.password");
        String path = System.getProperty("s3.suplimentalRouteTypesPath");
        S3Utility s3Utility = new S3Utility(s3Username,s3Password,S3Utility.getBucketFromS3Path(path));
        return s3Utility.get(S3Utility.getKeyFromS3Path(path));
    }

    // method to read in data from s3
    private void updateExpressRoutes(InputStream data) throws IOException {
        _routesToNycType.clear();

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
                    _routesToNycType.put(routeId, routeType);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid CSV format: " + e.getMessage());
        }
    }



    public boolean isRouteExpress(String routeId) {
        return _routesToNycType.get(routeId).equals(RouteType.EXPRESS);
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

}
