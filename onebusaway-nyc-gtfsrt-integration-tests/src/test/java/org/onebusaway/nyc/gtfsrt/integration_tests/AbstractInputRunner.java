package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.junit.Test;
import org.onebusaway.utility.DateLibrary;

import java.io.File;
import java.io.InputStream;

/**
 *
 */
public class AbstractInputRunner {


    public static final String INFERENCE_TYPE = "inference";
    public static final String PROTOCOL_BUFFER_TYPE = "pb";

    public AbstractInputRunner(String datasetId, String bundleId, String date) throws Exception {
        setBundle(bundleId, date);
        loadInference(datasetId);
        loadTimePredictions(datasetId);
        loadServiceAlerts(datasetId);
    }

    private void loadInference(String datasetId) throws Exception {
        new WebController().setVehicleLocationRecords(getInferenceInput(datasetId));

    }

    private void loadTimePredictions(String datasetId) throws Exception {
        String resourceName = getFilenameFromPrefix(datasetId, PROTOCOL_BUFFER_TYPE);
        new WebController().setTimePredictionRecords(getResourceAsStream(resourceName));
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
