package org.onebusaway.nyc.admin.service.bundle.task;

        import au.com.bytecode.opencsv.CSVReader;
        import net.lingala.zip4j.exception.ZipException;
        import org.onebusaway.geospatial.model.CoordinatePoint;
        import org.onebusaway.nyc.admin.model.BundleRequestResponse;
        import org.onebusaway.gtfs_transformer.GtfsTransformer;
        import org.onebusaway.nyc.util.impl.FileUtility;
        import org.onebusaway.nyc.util.impl.UrlUtility;
        import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
        import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
        import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
        import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Autowired;

        import java.io.*;
        import java.util.*;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;
        import net.lingala.zip4j.core.ZipFile;

public class NycGtfsModTask extends BaseModTask implements Runnable {

    private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);

    private BundleRequestResponse _requestResponse;
    private HashMap<String,CoordinatePoint> coordinatesForZone = new HashMap<String, CoordinatePoint>();

    @Autowired
    public void setRequestResponse(BundleRequestResponse requestResponse) {
        _requestResponse = requestResponse;
    }

    @Override
    public void run() {
        if(configurationService == null){
            ConfigurationServiceImpl configurationServiceImpl = new ConfigurationServiceImpl();
            configurationServiceImpl.setTransitDataManagerApiLibrary(new TransitDataManagerApiLibrary("tdm.dev.obanyc.com", 80, "/api"));
            try {
                configurationServiceImpl.updateConfigurationMap("zones","bronx,brooklyn,manhattan,mtabc,queens,staten-island,google-transit-mta-agency");
                configurationServiceImpl.updateConfigurationMap("bronx_coordinate","(40.845158,-73.88852)");
                configurationServiceImpl.updateConfigurationMap("brooklyn_coordinate","(40.664314,-73.94622)");
                configurationServiceImpl.updateConfigurationMap("manhattan_coordinate","(40.77181,-73.971405)");
                configurationServiceImpl.updateConfigurationMap("mtabc_coordinate","(0,0)");
                configurationServiceImpl.updateConfigurationMap("queens_coordinate","(40.738186,-73.78097)");
                configurationServiceImpl.updateConfigurationMap("staten-island_coordinate","(40.607594,-74.123276)");
                configurationServiceImpl.updateConfigurationMap("google-transit-mta-agency_coordinate","(-999,-999)");
                configurationServiceImpl.updateConfigurationMap("bronx_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/bronx_modifications.md");
                configurationServiceImpl.updateConfigurationMap("brooklyn_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/brooklyn_modifications.md");
                configurationServiceImpl.updateConfigurationMap("manhattan_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/manhattan_modifications.md");
                configurationServiceImpl.updateConfigurationMap("mtabc_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/mtabc_modifications.md");
                configurationServiceImpl.updateConfigurationMap("queens_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/queens_modifications.md");
                configurationServiceImpl.updateConfigurationMap("staten-island_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/staten-island_modifications.md");
                configurationServiceImpl.updateConfigurationMap("google-transit-mta-agency_modurl","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/google-transit-mta-agency_modifications.md");

//                configurationServiceImpl.setConfigurationValue("admin","staten-island_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/staten-island_transformations.md");configurationServiceImpl.setConfigurationValue("admin","bronx_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/bronx_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","brooklyn_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/brooklyn_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","manhattan_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/manhattan_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","mtabc_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/mtabc_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","queens_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/queens_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","staten-island_routes","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/staten-island_routes.md");
//                configurationServiceImpl.setConfigurationValue("admin","bronx_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/bronx_transformations.md");
//                configurationServiceImpl.setConfigurationValue("admin","brooklyn_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/brooklyn_transformations.md");
//                configurationServiceImpl.setConfigurationValue("admin","manhattan_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/manhattan_transformations.md");
//                configurationServiceImpl.setConfigurationValue("admin","mtabc_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/mtabc_transformations.md");
//                configurationServiceImpl.setConfigurationValue("admin","queens_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/queens_transformations.md");
//                configurationServiceImpl.setConfigurationValue("admin","staten-island_transformations","https://raw.githubusercontent.com/wiki/camsys/onebusaway-nyc/staten-island_transformations.md");
            } catch (Exception e) {
                e.printStackTrace();
            }
            configurationService = configurationServiceImpl;
        }
        String[] zones = getZones();
        getZoneCoordinates(zones);


        try {
            _log.info("GtfsModTask Starting");
            for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
                String agencyId = parseAgencyDir(gtfsBundle.getPath().getPath());
                    String zone = determineZone(gtfsBundle);
                    String modUrl = getModUrl(zone);
                _log.info("using modUrl=" + modUrl + " for zone " + zone + " and bundle " + gtfsBundle.getPath());
                    String oldFilename = gtfsBundle.getPath().getPath();
                    String transform = null;
                    String newFilename = runModifications(gtfsBundle, zone, modUrl, transform);
                _log.info("Transformed " + oldFilename + " to " + newFilename + " according to url " + getModUrl(agencyId));


            }
        } catch (Throwable ex) {
            _log.error("error modifying gtfs:", ex);
        } finally {
            _log.info("GtfsModTask Exiting");
        }
    }

    private String[] getZones(){
        return configurationService.getConfigurationValueAsString("zones","").
                replaceAll("\\s_","").split(",");
    }

    private void getZoneCoordinates(String[] zones){
        for (String zone : zones){
            String coordinateString = configurationService.getConfigurationValueAsString(zone+"_coordinate","(0,0)");
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
        for(Map.Entry<String,CoordinatePoint> entry: coordinatesForZone.entrySet()) {
            if (zoneCoordinate.distanceTo(entry.getValue())<2){
                _log.info("Zone Identified: " + entry.getKey() + " " + entry.getValue().toString());
                return entry.getKey();
            }
        }
        return "google-transit-mta-agency";
    }


    private String getTransform(String zone, String path) {return getUrl(zone,"transform"); }
    private String getModUrl(String zone) {return getUrl(zone,"modurl"); }
    private String getRoutesUrl(String zone) {return getUrl(zone,"routes"); }

    private String getUrl(String zone, String constant){
        try {
            return configurationService.getConfigurationValueAsString( zone+"_"+constant,"");
        } catch (Exception e) {}
        return null;
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
