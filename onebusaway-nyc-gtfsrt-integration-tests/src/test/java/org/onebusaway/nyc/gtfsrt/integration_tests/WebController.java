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

package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.onebusaway.utility.DateLibrary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
        return new ByteArrayInputStream(execute(new HttpGet(url)));

    }



    public InputStream get(Map<String, String> params, String apiPathNoParams) throws Exception {
        String port = getPort();
        String context = getContext();
        
        apiPathNoParams = addParamsToPath(apiPathNoParams, params);
        
        String url = "http://localhost:" + port + context
                + apiPathNoParams;


        System.out.println("url=" + url);
        return new ByteArrayInputStream(execute(new HttpGet(url)));

    }

    private void post(String apiPath, Map<String, String> params) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + apiPath;

        System.out.println("url=" + url);
        HttpPost postMethod = new HttpPost(url);
        List<NameValuePair> form = new ArrayList<NameValuePair>();
        for (String key : params.keySet()) {
            form.add(new BasicNameValuePair(key, params.get(key)));
        }
        postMethod.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));
        execute(postMethod);
    }

    private void post(String apiPath, InputStream data) throws Exception {
        String port = getPort();
        String context = getContext();
        String url = "http://localhost:" + port + context
                + apiPath;

        System.out.println("url=" + url);
        HttpPost postMethod = new HttpPost(url);
        // buffered so the request is sent with a Content-Length, as before
        postMethod.setEntity(new ByteArrayEntity(data.readAllBytes()));
        execute(postMethod);
    }

    /**
     * Executes the request, checks it succeeded, and returns the response
     * body. The body is read fully because the connection is closed here.
     */
    private byte[] execute(HttpUriRequest request) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            byte[] body = response.getEntity() == null
                    ? new byte[0] : EntityUtils.toByteArray(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyText = new String(body, StandardCharsets.ISO_8859_1);
            if (!bodyText.equals("OK") && 200 != statusCode)
                throw new Exception("Request failed! "
                        + request.getURI()
                        + ": (" + statusCode
                        + ":" + response.getStatusLine().getReasonPhrase() + ")");
            return body;
        }
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
