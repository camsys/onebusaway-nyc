/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.cancelled.CancelledTripService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyListener;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CancelledTripHttpListenerTask {

    protected static Logger _log = LoggerFactory.getLogger(CancelledTripHttpListenerTask.class);

    // in progress read connection timeout in millis
    private static final int TIMEOUT_CONNECTION = 5000;
    // socket connection timeout in millis
    private static final int TIMEOUT_SOCKET = 5000;

    @Autowired
    private ConfigurationService _configurationService;

    @Autowired
    private NycTransitDataService _tds;

    @Autowired
    private CancelledTripService _cancelledTripService;

    private Boolean isEnabled;

    private String capiUrl;

    private Integer capiRefreshInterval;

    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    public void setTds(NycTransitDataService _tds) {
        this._tds = _tds;
    }

    public String getCapiUrl() {
        return capiUrl;
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public Integer getCapiRefreshInterval() {
        return capiRefreshInterval;
    }

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @PostConstruct
    public void setup() {
        refreshConfig();

        if(!allowCapi()){
            return;
        }

        CapiWebServicePollerThread thread = new CapiWebServicePollerThread(getCapiUrl(), _tds, _cancelledTripService);

        _taskScheduler.scheduleWithFixedDelay(thread, TimeUnit.SECONDS.toMillis(getCapiRefreshInterval()));
    }

    @Refreshable(dependsOn = {"tds.enableCapi", "tds.capiUrl"})
    protected void refreshConfig() {
        capiUrl= _configurationService.getConfigurationValueAsString("tds.capiUrl", null);
        isEnabled = _configurationService.getConfigurationValueAsBoolean("tds.enableCapi", false);
        capiRefreshInterval = _configurationService.getConfigurationValueAsInteger("tds.capiRefreshIntervalSec", 30);
    }

    private boolean allowCapi() {
        if (!isEnabled()){
            _log.warn("capi is not enabled in configuration");
            return false;
        }
        if (StringUtils.isBlank(getCapiUrl())) {
            _log.warn("url not configured, Capi polling via webservice disabled.");
            return false;
        }
        return true;
    }

    public class CapiWebServicePollerThread extends Thread {
        private String url;
        private NycTransitDataService tds;
        private CancelledTripService cancelledTripService;

        private ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JaxbAnnotationModule())
                .registerModule(new JodaModule());


        public CapiWebServicePollerThread(String url, NycTransitDataService tds, CancelledTripService cancelledTripService) {
            this.url = url;
            this.tds = tds;
            this.cancelledTripService = cancelledTripService;
        }


        public void run() {
            if(!allowCapi()){
                return;
            }

            _log.info("retrieving feed at " + url);

            Map<AgencyAndId, NycCancelledTripBean> recordMap = getFeed();
            cancelledTripService.updateCancelledTrips(recordMap);

            _log.info("retreived {} cancelled trip records", recordMap.keySet().size());
        }

        // package private for unit tests
        Map<AgencyAndId, NycCancelledTripBean> getFeed() {
            Map<AgencyAndId, NycCancelledTripBean> results = new ConcurrentHashMap<>();
            HttpGet get = new HttpGet(url);
            try {
                HttpResponse response = getHttpClient().execute(get);
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                // we need to give a hint on how to load a map
                List<NycCancelledTripBean> list = mapper.readValue(reader,
                        new TypeReference<List<NycCancelledTripBean>>() {});
                // for our results we want fully qualified agency id to be the key
                for (NycCancelledTripBean cancelledTrip : list) {
                    if (isValidCancelledTrip(cancelledTrip)) {
                        results.put(AgencyAndId.convertFromString(cancelledTrip.getTrip()), cancelledTrip);
                    }
                }
                return results;

            } catch (IOException ioe) {
                _log.error("failure retrieving " + url + ", " + ioe, ioe);
            } catch (Throwable t) {
                _log.error("unexpected exception retrieving " + url, t);
            }

            return results;
        }

        private boolean isValidCancelledTrip(NycCancelledTripBean message) {
            try {
                if ((message.getStatus().equalsIgnoreCase("canceled") ||
                        message.getStatus().equalsIgnoreCase("cancelled")) &&
                        tds.getTrip(message.getTrip()) != null) {
                    return true;
                }
            } catch (ServiceException e) {
                _log.warn("Error retrieving cancelled trip", e);
            }
            return false;
        }

        // allow unit tests to override
        protected HttpClient getHttpClient() {
            HttpParams httpParams = new BasicHttpParams();
            // recover from bad network conditions
            HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_CONNECTION);
            HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
            HttpClient httpClient = new DefaultHttpClient(httpParams);

            return httpClient;
        }
    }
}
