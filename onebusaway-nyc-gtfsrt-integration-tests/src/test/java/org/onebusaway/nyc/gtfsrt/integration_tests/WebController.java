package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.onebusaway.utility.DateLibrary;

import java.io.InputStream;
import java.util.Date;

/**
 * Controller to absract actions against gtfsrt-webapp from the
 * integration tests.
 */
public class WebController {

    public void setBundle(String bundleId, Date date) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + "/change-bundle.do?bundleId="
                + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date);

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);

        validateSuccess(get);
    }


    public void setVehicleLocationRecords(InputStream data) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + "/input/vehicleLocationRecords";

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(new InputStreamRequestEntity(data));
        client.executeMethod(postMethod);
        validateSuccess(postMethod);

    }

    private void validateSuccess(HttpMethod method) throws Exception {
        String response = method.getResponseBodyAsString();
        if (!response.equals("OK") && 302 != method.getStatusCode())
            throw new Exception("Request failed! "
                    + method.getURI()
                    + ": (" + method.getStatusCode()
                    + ":" + method.getStatusText() + ")");

    }

    private String getPort() {
        return System.getProperty(
                "org.onebusaway.webapp.port", "9000");
    }
    private String getContext() {
        return System.getProperty(
                "org.onebusaway.webapp.context", "/onebusaway-nyc-gtfsrt-webapp");
    }

//    private static class InputPartSource implements PartSource{
//        private InputStream _streamData;
//        public InputPartSource(InputStream data) {
//            _streamData = data;
//        }
//        public long getLength() { return -1; }
//        public String getFileName() { return "vehicleLocationRecords.tsv"; }
//        public InputStream createInputStream() throws IOException {
//            return _streamData;
//        }
//    }
}
