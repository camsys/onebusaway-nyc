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
package org.onebusaway.nyc.transit_data_manager.api.apc;


import com.sun.jersey.api.spring.Autowire;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/apc")
@Component
@Autowire
/**
 * Proxy APC Raw Counts information.
 * Providing an oppourtunity for a stable API, caching, etc.
 */
public class ApcResource {

    @Autowired
    private ConfigurationService _configurationService;

    private static Logger _log = LoggerFactory.getLogger(ApcResource.class);
    // in progress read connection timeout in millis
    private static final int TIMEOUT_CONNECTION = 5000;
    // socket connection timeout in millis
    private static final int TIMEOUT_SOCKET = 5000;

    public ApcResource() {
    }

    @Path("/list/all")
    @GET
    @Produces("application/json")
    public Response getAll() throws IOException {
        String url = getRawCountsUrl();
        if (url == null) {
            _log.error("raw counts not configured:  set 'tdm.apcRawCountUrl' if this is desired");
            return Response.status(404).build();
        }

        HttpResponse webServiceCall = getHttpClient().execute(new HttpGet(url));
        try {
            return Response.ok(webServiceCall.getEntity().getContent()).build();
        } catch (Exception any) {
            _log.error("serialization failed: ", any);
            return Response.serverError().build();
        }
    }

    private String getRawCountsUrl() {
        if (_configurationService != null) {
            try {
                return _configurationService.getConfigurationValueAsString("tdm.apcRawCountUrl", null);
            } catch (RemoteConnectFailureException e){
                _log.error("apc raw count url lookup failed:", e);
                return null;
            }
        }
        return null;
    }

    private HttpClient getHttpClient() {
        HttpParams httpParams = new BasicHttpParams();
        // recover from bad network conditions
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_CONNECTION);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        return httpClient;
    }

}
