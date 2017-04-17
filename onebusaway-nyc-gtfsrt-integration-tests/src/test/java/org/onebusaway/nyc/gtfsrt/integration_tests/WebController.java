package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.onebusaway.utility.DateLibrary;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to absract actions against gtfsrt-webapp from the
 * integration tests.
 */
public class WebController {

    public void setBundle(String bundleId, Date date) throws Exception {
        get("/change-bundle.do?bundleId="
                + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date));
    }

    public void setVehicleLocationRecords(InputStream data) throws Exception {
        post("/input/vehicleLocationRecords", data);
    }

    public void setTimePredictionRecordsTime(Date serviceDate) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", String.valueOf(serviceDate.getTime()));
        post("/input/timePredictionRecordsTime", params);
    }

    public void setTimePredictionRecords(InputStream data) throws Exception {
        post("/input/timePredictionRecords", data);
    }

    public InputStream get(String apiPathWithEncodedParams) throws Exception {
        String port = getPort();
        String context = getContext();

        String url = "http://localhost:" + port + context
                + apiPathWithEncodedParams;

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);

        validateSuccess(get);

        return get.getResponseBodyAsStream();

    }



    public InputStream get(Map<String, String> params, String apiPathNoParams) throws Exception {
        String port = getPort();
        String context = getContext();
        
        apiPathNoParams = addParamsToPath(apiPathNoParams, params);
        
        String url = "http://localhost:" + port + context
                + apiPathNoParams;


        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        for (String key : params.keySet()) {
            client.getParams().setParameter(key, params.get(key));
        }
        client.executeMethod(get);

        validateSuccess(get);

        return get.getResponseBodyAsStream();

    }

    private void post(String apiPath, Map<String, String> params) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + apiPath;

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        for (String key : params.keySet()) {
            postMethod.addParameter(key, params.get(key));
        }
        client.executeMethod(postMethod);
        validateSuccess(postMethod);
    }

    private void post(String apiPath, InputStream data) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + apiPath;

        System.out.println("url=" + url);
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(new InputStreamRequestEntity(data));
        client.executeMethod(postMethod);
        validateSuccess(postMethod);
    }

    private void validateSuccess(HttpMethod method) throws Exception {
        String response = method.getResponseBodyAsString();
        if (!response.equals("OK") && 200 != method.getStatusCode())
            throw new Exception("Request failed! "
                    + method.getURI()
                    + ": (" + method.getStatusCode()
                    + ":" + method.getStatusText() + ")");

    }

    private String addParamsToPath(String path, Map<String, String> params) {
        StringBuffer sb = new StringBuffer();
        sb.append(path);
        boolean containsParams = false;
        if (params != null && !params.isEmpty()) {
            if (path.contains("?"))
                containsParams = true;

            for (String key : params.keySet()) {
                if (!containsParams) {
                    sb.append("?").append(key).append("=").append(params.get(key));
                    containsParams = true;
                } else {
                    sb.append("&").append(key).append("=").append(params.get(key));
                }
            }

        }
        return sb.toString();
    }

    private String getPort() {
        return System.getProperty(
                "org.onebusaway.webapp.port", "9000");
    }
    private String getContext() {
        return System.getProperty(
                "org.onebusaway.webapp.context", "/onebusaway-nyc-gtfsrt-webapp");
    }

}
