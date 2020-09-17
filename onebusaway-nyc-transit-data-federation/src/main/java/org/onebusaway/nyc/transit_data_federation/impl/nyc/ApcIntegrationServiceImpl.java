/**
 * Copyright (C) 2017 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.ApcQueueListenerTask;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyListener;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support levels of crowding and / or raw passenger count integration
 * and inject into OneBusAway TDS.
 */
public class ApcIntegrationServiceImpl extends ApcQueueListenerTask {

    // how recent the occupancy record needs to be for usage
    public static final int MAX_AGE_MILLIS = 6 * 60 * 1000; // 6 minutes

    // in progress read connection timeout in millis
    private static final int TIMEOUT_CONNECTION = 5000;
    // socket connection timeout in millis
    private static final int TIMEOUT_SOCKET = 5000;

    @Autowired
    private NycTransitDataService _tds;

    @Autowired
    private VehicleOccupancyListener _listener;

    private Map<AgencyAndId, VehicleOccupancyRecord> _vehicleToQueueOccupancyCache = new ConcurrentHashMap<AgencyAndId, VehicleOccupancyRecord>();

    /**
     * Poll webservice at getRawCountUrl() for raw counts.  Alternatively
     * pull APC levels off queue if false.
     */
    private boolean getRawCountsViaWebService() {
        return getRawCountUrl() != null;
    }


    public String getRawCountUrl() {
        return _configurationService.getConfigurationValueAsString("tds.apcRawCountUrl", null);
    }

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @PostConstruct
    public void setup() {
        super.setup();
        if (!getRawCountsViaWebService()) {
            _log.error("url not configured, Raw Count polling via webservice disabled.");
            return;
        }


        RawCountWebServicePollerThread thread = new RawCountWebServicePollerThread(_tds, _configurationService, _listener, _vehicleToQueueOccupancyCache, getRawCountUrl());
        _taskScheduler.scheduleWithFixedDelay(thread, 30 * 1000);
    }

    @Override
    protected void processResult(NycVehicleLoadBean message, String contents) {
        _log.debug("found message=" + message + " for contents=" + contents);
        VehicleOccupancyRecord vor = toVehicleOccupancyRecord(message);
        if (!getRawCountsViaWebService()) {
            _listener.handleVehicleOccupancyRecord(vor);
        } else {
            //merge this record with webservice results otherwise raw counts will be quashed
            _vehicleToQueueOccupancyCache.put(vor.getVehicleId(), vor);
        }
    }

    private VehicleOccupancyRecord toVehicleOccupancyRecord(NycVehicleLoadBean message) {
        VehicleOccupancyRecord vor = new VehicleOccupancyRecord();
        vor.setOccupancyStatus(message.getLoad());
        vor.setRawCount(message.getEstLoad());
        vor.setCapacity(message.getEstCapacity());
        vor.setTimestamp(new Date(message.getRecordTimestamp()));
        vor.setVehicleId(AgencyAndId.convertFromString(message.getVehicleId()));
        vor.setRouteId(message.getRoute());
        vor.setDirectionId(message.getDirection());
        return vor;
    }

    public static class RawCountWebServicePollerThread extends Thread {
        private NycTransitDataService tds;
        private ConfigurationService configService;
        private VehicleOccupancyListener listener;
        private Map<AgencyAndId, VehicleOccupancyRecord> vehicleToQueueOccupancyCache;
        private String url;
        private long maxEarlyMessageMillis = 0;
        private long maxLateMessageMillis = 0;
        private int totalCount = 0;
        private int earlyCount = 0;
        private int lateCount = 0;


        public RawCountWebServicePollerThread(NycTransitDataService tds,
                                              ConfigurationService cs,
                                              VehicleOccupancyListener listener,
                                              Map<AgencyAndId, VehicleOccupancyRecord> vehicleToQueueOccupancyCache,
                                              String url) {
            this.tds = tds;
            this.configService = cs;
            this.listener = listener;
            this.vehicleToQueueOccupancyCache = vehicleToQueueOccupancyCache;
            this.url = url;
        }


        public void run() {
            prepareForRun();
            _log.info("retrieving feed at " + url);

            Map<AgencyAndId, ApcLoadData> recordMap = getFeed();

            // here we assume the web service feed will be the superset of data
            for (AgencyAndId vehicleId : recordMap.keySet()) {
                totalCount++;
                VehicleOccupancyRecord vor = merge(vehicleId, vehicleToQueueOccupancyCache, recordMap.get(vehicleId));
                listener.handleVehicleOccupancyRecord(vor);
            }

            if (totalCount > 0) {
                _log.info("APC run complete with "
                        + earlyCount + " (" + (earlyCount * 100 / totalCount)
                        + "% earlier than " + maxEarlyMessageMillis/1000 + "s)" + " early, "
                        + lateCount + " (" + (lateCount * 100 / totalCount)
                        + "% later than " + maxLateMessageMillis/1000 + "s)" + " late"
                        + " of " + totalCount + " entries");
            } else {
                _log.info("APC run complete with nothing to do");
            }

        }

        // package private for unit tests
        Map<AgencyAndId, ApcLoadData> getFeed() {
            Map<AgencyAndId, ApcLoadData> results = new HashMap<AgencyAndId, ApcLoadData>();
            HttpGet get = new HttpGet(url);
            try {
                HttpResponse response = getHttpClient().execute(get);
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                Gson gson = new Gson();
                // we need to give a hint on how to load a map
                Type apcMapType = new TypeToken<Map<String, ApcLoadData>>() {}.getType();
                Map<String, ApcLoadData> map = gson.fromJson(reader, apcMapType);
                // for our results we want fully qualified agency id to be the key
                for (String key : map.keySet()) {
                    ApcLoadData ald = map.get(key);
                    if (ald.getVehicleAsId() != null)
                        results.put(ald.getVehicleAsId(), ald);
                }
                return results;

            } catch (IOException ioe) {
                _log.error("failure retrieving " + url + ", " + ioe, ioe);
            } catch (Throwable t) {
                _log.error("unexpected exception retrieving " + url, t);
            }

            return results;
        }

        private  VehicleOccupancyRecord merge(AgencyAndId vehicleId,
                                              Map<AgencyAndId, VehicleOccupancyRecord> cache,
                                              ApcLoadData additionalApcData) {
            VehicleOccupancyRecord queueRecord = cache.get(vehicleId);

            // CASE 1:  no ApcData element from webservice, return whatever matches from cache
            if (additionalApcData == null  || !isTimely(vehicleId, additionalApcData.getTimestamp())) {
                return queueRecord;
            }
            // CASE 2:  no enumeration from queue, return ApcData as VehicleOccupancyRecord
            if (queueRecord == null || !isTimely(vehicleId, queueRecord.getTimestamp().getTime())) {
                return toVehicleOccupancyRecord(additionalApcData);
            }
            // CASE 3:  we have both, merge raw counts/capacity into queue record and return
            VehicleOccupancyRecord vor = queueRecord.deepCopy();
            vor.setRawCount(additionalApcData.getEstLoadAsInt());
            vor.setCapacity(additionalApcData.getEstCapacityAsInt());
            vor.setVehicleId(vehicleId);
            vor.setTimestamp(new Date(additionalApcData.getTimestamp()));
            return vor;

        }

        private boolean isTimely(AgencyAndId vehicleId, long time) {
            long delta = System.currentTimeMillis() - time;
            long early = getMaxEarlyMessageMillis();
            long late = getMaxLateMessageMillis();
            if (delta < -1 * early) {
                earlyCount++;
                _log.debug("dropping v " + vehicleId + " as its " + (delta/60000) + " mins early");
                return false;
            }
            if (late > delta) {
                lateCount++;
                _log.debug("dropping v" + vehicleId + " as its " + (delta/60000) + " mins late");
                return false;
            }
            return true;
        }

        private void prepareForRun() {
            // look up these values on each "run" (every 30 seconds)
            maxEarlyMessageMillis = configService.getConfigurationValueAsInteger("tds.apcAllowableEarly", 60*1000);
            maxLateMessageMillis = configService.getConfigurationValueAsInteger("tds.apcAllowableLate", 6*60*1000);
            totalCount = 0;
            earlyCount = 0;
            lateCount = 0;
        }


        private long getMaxEarlyMessageMillis() {
            return maxEarlyMessageMillis;
        }

        private long getMaxLateMessageMillis() {
            return maxLateMessageMillis;
        }

        private VehicleOccupancyRecord toVehicleOccupancyRecord(ApcLoadData data) {
            VehicleOccupancyRecord vor = new VehicleOccupancyRecord();
            vor.setVehicleId(data.getVehicleAsId());
            vor.setTimestamp(new Date(data.getTimestamp()));
            vor.setCapacity(data.getEstCapacityAsInt());
            vor.setRawCount(data.getEstLoadAsInt());
            return vor;
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
