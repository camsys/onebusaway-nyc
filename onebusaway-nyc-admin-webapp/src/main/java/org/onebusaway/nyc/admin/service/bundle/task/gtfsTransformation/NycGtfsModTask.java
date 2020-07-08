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

package org.onebusaway.nyc.admin.service.bundle.task.gtfsTransformation;

        import au.com.bytecode.opencsv.CSVReader;
        import net.lingala.zip4j.exception.ZipException;
        import org.onebusaway.nyc.admin.model.BundleRequestResponse;
        import org.onebusaway.gtfs_transformer.GtfsTransformer;
        import org.onebusaway.nyc.admin.service.bundle.task.SimpleFeedVersionStrategy;
        import org.onebusaway.nyc.admin.util.FileUtils;
        import org.onebusaway.nyc.util.impl.FileUtility;
        import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
        import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
        import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Autowired;

        import java.io.*;
        import java.util.*;
        import java.util.stream.Collectors;

        import net.lingala.zip4j.core.ZipFile;

        import javax.naming.ConfigurationException;

public class NycGtfsModTask extends BaseModTask implements Runnable {

    private static Logger _log = LoggerFactory.getLogger(NycGtfsModTask.class);

    private BundleRequestResponse _requestResponse;
    private HashMap<String,CoordinatePoint> coordinatesForZone = new HashMap<String, CoordinatePoint>();
    private final int CONFIG_KEY_ARG_ZONE = 0;
    private final int CONFIG_KEY_ARG_COORDINATE = 1;
    private final int CONFIG_KEY_ARG_TRANSFORMATION_URL = 2;
    private final int CONFIG_KEY_ARG_TRANSFORMATION_EXCEPTIONS = 3;
    private final int CONFIG_KEY_ARG_DISTANCE_FROM_ZONE_CENTER = 4;
    private final int CONFIG_KEY_ARG_DELIMETER = 5;
    private final String DEFAULT_ZONES_VALUES = "bronx,brooklyn,manhattan,mtabc,queens,staten-island,google-transit-mta-agency";
    private final String DEFAULT_CONFIG_KEYS = "zones,coordinate,modurl,zones-to-avoid-transforming,maxDistancetoZoneCenter,_";
    private final String DEFAULT_CATAGORIZED_AS_DEFAULT_ZONE = "google-transit-mta-agency";
    private static final Map<String, String> DEFAULT_TRANSFORMATION_URLS =
            Arrays.stream(new String[][] {
                    {"bronx_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/bronx_modifications.md"},
                    {"brooklyn_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/brooklyn_modifications.md"},
                    {"manhattan_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/manhattan_modifications.md"},
                    {"mtabc_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/mtabc_modifications.md"},
                    {"queens_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/queens_modifications.md"},
                    {"staten-island_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/staten-island_modifications.md"},
                    {"google-transit-mta-agency_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/google-transit-mta-agency_modifications.md"},
            }).collect(Collectors.toMap(mapEntry -> mapEntry[0], mapEntry -> mapEntry[1]));

    private static final Map<String, String> DEFAULT_ZONE_COORDINATES =
            Arrays.stream(new String[][] {
                    {"bronx_coordinate","(40.845158,-73.88852)"},
                {"brooklyn_coordinate","(40.664314,-73.94622)"},
                {"manhattan_coordinate","(40.77181,-73.971405)"},
                {"mtabc_coordinate","(40.727154,-73.859116)"},
                {"queens_coordinate","(40.738186,-73.78097)"},
                {"staten-island_coordinate","(40.607594,-74.123276)"},
                {"google-transit-mta-agency_coordinate","(-999,-999)"},
            }).collect(Collectors.toMap(mapEntry -> mapEntry[0], mapEntry -> mapEntry[1]));
    private String _zone;
    private String _coordinate;
    private String _transformationUrl;
    private String _transformationExceptions;
    private String _distanceFromZoneCenter;
    private String _delimiter;
    private String DEFAULT_TDS_VALUE_LOCATION_OF_ROUTE_MAPPING = "file_name_zone_route_mapping";
    private String DEFAULT_TDS_VALUE_WRITE_ROUTE_MAPPING = "should_zone_route_mapping";
    private String DEFAULT_LOCATION_OF_RAW_ROUTE_MAPPING = "ListOfRoutesInGtfs.txt";
    private String DEFAULT_LOCATION_OF_ROUTE_MAPPING = "routesByZone.txt";
    private boolean DEFAULT_WRITE_ROUTE_MAPPING = true;



    @Autowired
    public void setRequestResponse(BundleRequestResponse requestResponse) {
        _requestResponse = requestResponse;
    }

    @Override
    public void run() {
        _log.info("starting NycGtfsModTask");
        if(configurationService == null){
            ConfigurationServiceImpl configurationServiceImpl = new ConfigurationServiceImpl();
            configurationServiceImpl.setTransitDataManagerApiLibrary(new TransitDataManagerApiLibrary("tdm.dev.obanyc.com", 80, "/api"));
            configurationService = configurationServiceImpl;
        }
        try{
            super.setWriteZoneRouteMapping(getWriteRouteMapping());
            super.setRouteMappingOutputName(getLocationOfRouteMapping());
            getModTaskConfigKeys();
            String[] zones = getZones();
            getZoneCoordinates(zones);
            List zonesToAvoidTransforming = Arrays.asList(zonesToAvoidTransforming());
            _log.info("GtfsModTask Starting");
            for (GtfsBundle gtfsBundle : getGtfsBundles(_applicationContext).getBundles()) {
                File outputDestination = gtfsBundle.getPath().getAbsoluteFile();

                File tmpDir = new File(gtfsBundle.getPath().getParentFile(), "tmpTransformationFolder");
                tmpDir.mkdir();
                File workingLocation = new File(tmpDir + "/" + gtfsBundle.getPath().getName());
                FileUtils.copyFile(gtfsBundle.getPath(), workingLocation);
                gtfsBundle.setPath(workingLocation);


                String agencyId = parseAgencyDir(gtfsBundle.getPath().getPath());
                String zone = determineZone(gtfsBundle);
                if (zonesToAvoidTransforming.contains(zone)) {
                    continue;
                }
                String modUrl = getModUrl(zone);
                _log.info("using modUrl=" + modUrl + " for zone " + zone + " and bundle " + gtfsBundle.getPath());
                String oldFilename = gtfsBundle.getPath().getPath();
                String transform = null;
                if(_requestResponse.getRequest().getPredate()){
                    transform ="{\"op\":\"transform\", \"class\":\"org.onebusaway.gtfs_transformer.impl.PredateCalendars\"}";
                }
                String newFilename = runModifications(gtfsBundle, zone, modUrl, transform);
                _log.info("Transformed " + oldFilename + " to " + newFilename + " according to url " + getModUrl(agencyId));

                outputDestination.delete();
                outputDestination = new File(outputDestination.getParentFile(), gtfsBundle.getPath().getName());
                FileUtils.copyFile(gtfsBundle.getPath(), new File(outputDestination.getParentFile(), gtfsBundle.getPath().getName()));
                gtfsBundle.setPath(outputDestination);
                if(super.getWriteZoneRouteMapping()){
                    mergeZoneMapping(zone,outputDestination.getParentFile());
                }
            }
    }catch (Exception ex) {
        _log.error("error modifying gtfs:", ex);
        _requestResponse.getResponse().setException(new RuntimeException(ex.getMessage()  + ":  " + ex.getStackTrace()));
    } finally {
        _log.info("GtfsModTask Exiting");
    }
    }

    private void mergeZoneMapping(String zone,File workingDir){
        File mergedOutputs = new File (workingDir,DEFAULT_LOCATION_OF_ROUTE_MAPPING);
        File rawOutput = new File (workingDir,super.getRouteMappingOutputName());
        BufferedWriter writer;
        try {
            if (!mergedOutputs.isFile()) {
                writer = new BufferedWriter(new FileWriter(mergedOutputs));
                writer.write("zone_name, [route1_agency]***[route1_routeid],[route2_agency]***[route2_routeid]...\n");
                writer.close();
            }
            writer = new BufferedWriter(new FileWriter(mergedOutputs, true));
            BufferedReader reader = new BufferedReader((new FileReader((rawOutput))));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(zone+","+line + "\n");
            }
            reader.close();
            writer.close();
        }
        catch (IOException exception){
            _log.error("Issue writing listOfRoutes in ModTask/NycGtfsModTask for later use in FixedRouteValidationTask");
        }
        rawOutput.delete();
    }

    private void getModTaskConfigKeys() {
        String configKeysString = configurationService.getConfigurationValueAsString("ModTaskConfigKeys", DEFAULT_CONFIG_KEYS);
//                    When setting ModTaskConfigKeys in TDM. Please include the following keys:
//                    key for comma seperated list of zones
//                    key for coordinates: [zone][delimeterKey][coordinateKey]
//                    key for transformations: [zone][delimeterKey][transformationKey]
//                    key for comma seperated list of zones to avoid transforming
//                    key for maximum distance zone center may be to zone coordinates
//                    value used for delimeter

        String[] configKeys = configKeysString.replaceAll("\\s_","").split(",");
        _zone = configKeys[CONFIG_KEY_ARG_ZONE];
        _coordinate = configKeys[CONFIG_KEY_ARG_COORDINATE];
        _transformationUrl = configKeys[CONFIG_KEY_ARG_TRANSFORMATION_URL];
        _transformationExceptions = configKeys[CONFIG_KEY_ARG_TRANSFORMATION_EXCEPTIONS];
        _distanceFromZoneCenter = configKeys[CONFIG_KEY_ARG_DISTANCE_FROM_ZONE_CENTER];
        _delimiter = configKeys[CONFIG_KEY_ARG_DELIMETER];
    }

    private float getMaxDistance(){
        return configurationService.getConfigurationValueAsFloat(_distanceFromZoneCenter, (float) .05);
    }

    private String[] getZones(){
        return configurationService.getConfigurationValueAsString(_zone,DEFAULT_ZONES_VALUES).
                replaceAll("\\s_","").split(",");
    }

    private String getLocationOfRouteMapping(){
        return configurationService.getConfigurationValueAsString(DEFAULT_TDS_VALUE_LOCATION_OF_ROUTE_MAPPING, DEFAULT_LOCATION_OF_RAW_ROUTE_MAPPING);
    }
    private boolean getWriteRouteMapping(){
        return configurationService.getConfigurationValueAsBoolean(DEFAULT_TDS_VALUE_WRITE_ROUTE_MAPPING, DEFAULT_WRITE_ROUTE_MAPPING);
    }

    private String[] zonesToAvoidTransforming(){
        return configurationService.getConfigurationValueAsString(_transformationExceptions,"google-transit-mta-agency").
                replaceAll("\\s_","").split(",");
    }

    private void getZoneCoordinates(String[] zones){
        for (String zone : zones){
            String configurationItemKey = zone+_delimiter+_coordinate;
            String coordinateString = configurationService.getConfigurationValueAsString(configurationItemKey,null);
            if (coordinateString == null){
                _log.error("Looking for Zone Coordinates. Expected to find [zone][delimiter][coordtinate] : ([floatLatitude],[floatLongitude]) "
                        + " . Found null when searching for " + configurationItemKey + ". Using defaults.");
                coordinateString = DEFAULT_ZONE_COORDINATES.get(configurationItemKey);
            }
            coordinateString = coordinateString.replaceAll("[\\(\\[\\)\\]]","");
            String[] coordinateStringSplit = coordinateString.split(",");
            CoordinatePoint coordinate = new CoordinatePoint(
                    Float.parseFloat(coordinateStringSplit[0]),
                    Float.parseFloat(coordinateStringSplit[1]));
            coordinatesForZone.put(zone,coordinate);
        }
    }

    private String determineZone(GtfsBundle bundle){
        String zone = null;

        Set<String> routes = new HashSet<String>();
        //parse and pull out routes
        try {
            FileInputStream fis = new FileInputStream(bundle.getPath());
            FileUtility utility = new FileUtility();
            File outputFolder = new File(bundle.getPath().getAbsolutePath().replace(".zip",""));
            outputFolder.mkdir();
            new ZipFile(bundle.getPath()).extractAll(outputFolder.getAbsolutePath());
            CSVReader reader = new CSVReader(new FileReader(outputFolder.getAbsolutePath()+ "/stops.txt"));
            int stop_lat_collumn = -1;
            int stop_lon_collumn = -1;
            String[] nextLine = reader.readNext();
            ArrayList<Float> latitudes = new ArrayList<Float>();
            ArrayList<Float> longitudes = new ArrayList<Float>();
            while (nextLine != null) {
                if(stop_lat_collumn == -1 || stop_lon_collumn == -1){
                    stop_lat_collumn = Arrays.asList(nextLine).indexOf("stop_lat");
                    stop_lon_collumn = Arrays.asList(nextLine).indexOf("stop_lon");
                    nextLine = reader.readNext();
                    continue;
                }
                // nextLine[] is an array of values from the line
                float lat = Float.parseFloat(nextLine[stop_lat_collumn]);
                float lon = Float.parseFloat(nextLine[stop_lon_collumn]);
                latitudes.add(lat);
                longitudes.add(lon);
                nextLine = reader.readNext();
            }
            Collections.sort(latitudes);
            Collections.sort(longitudes);
            float latMedian = -999;
            float lonMedian = -999;
            if(latitudes.size() > 1 && longitudes.size() > 1) {
                latMedian = latitudes.get((int) latitudes.size() / 2);
                lonMedian = longitudes.get((int) longitudes.size() / 2);
            }
            CoordinatePoint median = new CoordinatePoint(latMedian,lonMedian);
            zone = categorizeByZone(median,outputFolder);
        }
        catch(IOException | ZipException exception){
            _log.error("Error reading: " + bundle.getPath().getAbsolutePath() +
                    " during gtfs transformation process",exception);
        }

        return zone;
    }

    private String categorizeByZone(CoordinatePoint zoneCoordinate, File path){
        _log.info("Median coordinate for: "+ path.getName() +
                " is at: " +zoneCoordinate.toString());
        float shortestDistance = getMaxDistance();
        String zone = DEFAULT_CATAGORIZED_AS_DEFAULT_ZONE;
        for(Map.Entry<String,CoordinatePoint> entry: coordinatesForZone.entrySet()) {
            if (zoneCoordinate.distanceTo(entry.getValue()) < shortestDistance){
                _log.info("Likely Zone Identified: " + entry.getKey() + " " + entry.getValue().toString());
                zone = entry.getKey();
            }
        }
        return zone;
    }


    private String getModUrl(String zone){
        String configurationItemKey = zone+_delimiter+_transformationUrl;
        String urlString = configurationService.getConfigurationValueAsString( configurationItemKey,null);
        if (urlString == null){
            urlString = DEFAULT_TRANSFORMATION_URLS.get(configurationItemKey);
            _log.error("Looking for URL for Transformations. Expected to find [zone][delimiter][url] -> address "
                    + " . Found null when searching for " + configurationItemKey + ". Using defaults.");
        }
        return urlString;
    }

    @Override
    public void addExtraMods(GtfsTransformer mod) {
        SimpleFeedVersionStrategy strategy = new SimpleFeedVersionStrategy();
        String name = requestResponse.getRequest().getBundleName();
        strategy.setVersion(name);
        mod.addTransform(strategy);
    }


    private class CoordinatePoint {
        private float lat;
        private float lon;
        CoordinatePoint(float lat, float lon){
            this.lat = lat;
            this.lon = lon;
        }

        public float getLon() {
            return lon;
        }

        public float getLat() {
            return lat;
        }

        public float distanceTo (CoordinatePoint distantPoint){
            float latDistance = this.lat - distantPoint.getLat();
            float lonDistance = this.lon - distantPoint.getLon();
            return (float) Math.sqrt(latDistance*latDistance + lonDistance*lonDistance);
        }

        @Override
        public String toString() {
            return "("+ this.lat + "," + this.lon +")";
        }
    }

}
