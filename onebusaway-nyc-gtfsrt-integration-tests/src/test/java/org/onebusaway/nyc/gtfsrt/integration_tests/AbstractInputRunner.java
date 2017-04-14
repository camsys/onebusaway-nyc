package org.onebusaway.nyc.gtfsrt.integration_tests;

import com.google.transit.realtime.GtfsRealtime;
import org.junit.Test;
import org.onebusaway.utility.DateLibrary;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class AbstractInputRunner {


    public static final String INFERENCE_TYPE = "inference";
    public static final String PROTOCOL_BUFFER_TYPE = "tripUpdate";

    public AbstractInputRunner(String datasetId, String bundleId, String date) throws Exception {
        setBundle(bundleId, date);
        loadInference(datasetId);
        loadTimePredictions(datasetId, date);
        loadServiceAlerts(datasetId);
        verify(datasetId,  bundleId, date);
    }

    private void verify(String datasetId, String bundleId, String date) throws Exception {
        // load the vehicle
        String vehicleId = datasetId.substring(datasetId.lastIndexOf("_")+1);
        assertEquals("7198", vehicleId);
        // load the trip
        // load the stops
        // query gtfs-rt
        // parse
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", String.valueOf(DateLibrary.getIso8601StringAsTime(date).getTime()));
        GtfsRealtime.FeedMessage feed = readUrl(params);
        assertNotNull(feed);
        assertEquals(1, feed.getEntityCount());
        // assert vehicle
        // assert trip
        // assert each prediction
    }

    private GtfsRealtime.FeedMessage readUrl(Map<String, String> params) throws Exception {
        InputStream is = new WebController().get(params, "/tripUpdates");
        return GtfsRealtime.FeedMessage.parseFrom(is);
    }

    private void loadInference(String datasetId) throws Exception {
        new WebController().setVehicleLocationRecords(getInferenceInput(datasetId));
    }

    private void loadTimePredictions(String datasetId, String date) throws Exception {
        String resourceName = getFilenameFromPrefix(datasetId, PROTOCOL_BUFFER_TYPE);
        WebController wc = new WebController();

        wc.setTimePredictionRecordsTime(DateLibrary.getIso8601StringAsTime(date));
        wc.setTimePredictionRecords(getResourceAsStream(resourceName));
    }

    private InputStream getInferenceInput(String prefix) {
        String resourceName = getInferenceFilename(prefix);
        return getResourceAsStream(resourceName);
    }
    private InputStream getResourceAsStream(String resourceName) {
        System.out.println("resource=" + resourceName);
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        if (is == null) {
            throw new NullPointerException(
                    "resource '" + resourceName
                    + "' did not match data on class path");
        }
        return this.getClass().getResourceAsStream(resourceName);

    }

    private String getInferenceFilename(String prefix) {
        return getFilenameFromPrefix(prefix, INFERENCE_TYPE);
    }

    private String getFilenameFromPrefix(String prefix, String type) {
        return File.separator
                + "data"
                + File.separator
                + prefix
                + File.separator
                + type
                + getExtensionForType(type);

    }
    private String getExtensionForType(String type) {
        if (INFERENCE_TYPE.equals(type)) {
            return ".tsv";
        } else if (PROTOCOL_BUFFER_TYPE.equals(type)) {
            return ".pb";
        }
        return ".txt";
    }


    private void loadServiceAlerts(String datasetId) {

    }

    public void setBundle(String bundleId, String date) throws Exception {
        new WebController().setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
    }



    @Test
    public void testRun() throws Throwable {
        System.out.println("here we go!");
    }

}
