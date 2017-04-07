package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.onebusaway.utility.DateLibrary;

import java.util.Date;

/**
 *
 */
public class AbstractInputRunner {



    public AbstractInputRunner(String datasetId, String bundleId, String date) throws Exception {
        //setBundle(bundleId, date);
        loadInference(datasetId);
        loadTimePredictions(datasetId);
        loadServiceAlerts(datasetId);
    }

    private void loadInference(String datasetId) {

    }

    private void loadTimePredictions(String datasetId) {

    }

    private void loadServiceAlerts(String datasetId) {

    }

    public void setBundle(String bundleId, String date) throws Exception {
        setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
    }

    public void setBundle(String bundleId, Date date) throws Exception {
        String port = System.getProperty(
                "org.onebusaway.webapp.port", "8282");
        String context = System.getProperty(
                "org.onebusaway.webapp.context", "/onebusaway-nyc-gtfsrt-webapp");
        String url = "http://localhost:" + port + context
                + "/change-bundle.do?bundleId="
                + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date);

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);

        String response = get.getResponseBodyAsString();
        if (!response.equals("OK"))
            throw new Exception("Bundle switch failed! (" + get.getStatusCode() + ":" + get.getStatusText() + ")");
    }

    @Test
    public void testRun() throws Throwable {
        System.out.println("here we go!");
    }

}
