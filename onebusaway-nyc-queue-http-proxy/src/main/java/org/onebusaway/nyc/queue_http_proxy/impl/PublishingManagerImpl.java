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

package org.onebusaway.nyc.queue_http_proxy.impl;

import org.onebusaway.nyc.queue.DNSResolver;
import org.onebusaway.nyc.queue.IPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class PublishingManagerImpl implements PublishingManager{

    private static Logger _log = LoggerFactory.getLogger(PublishingManagerImpl.class);

    private final DateFormat dateFormat;

    private Map<String, Date> lastKnownVehicleRecords = new ConcurrentHashMap<>(10000);

    private String highFrequencyVehiclesList;

    private Set<String> highFrequencyVehicles;

    @Autowired
    @Qualifier("publisher")
    private IPublisher publisher;

    @Autowired
    @Qualifier("high_freq_publisher")
    private IPublisher highFreqPublisher;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    protected DNSResolver _resolver = null;


    public PublishingManagerImpl() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @PostConstruct
    public void setup(){
        String[] vehiclesList = highFrequencyVehiclesList.split("\\s*,\\s*");
        highFrequencyVehicles = new HashSet<>(Arrays.asList(vehiclesList));
        startDNSCheckThread();

    }

    public synchronized Date parseDate(String date) throws ParseException {
        return dateFormat.parse(date);
    }

    public Set<String> getHighFrequencyRoutes() {
        return highFrequencyVehicles;
    }

    public void setHighFrequencyVehicles(Set<String> highFrequencyVehicles) {
        this.highFrequencyVehicles = highFrequencyVehicles;
    }

    public void setHighFrequencyVehiclesList(String highFrequencyVehiclesList) {
        this.highFrequencyVehiclesList = highFrequencyVehiclesList;
    }

    @Override
    public void send(JsonNode message) throws ExecutionException, ParseException {
        JsonNode ccLocationReport = message.get("CcLocationReport");
        String vehicleId = getVehicleId(ccLocationReport);

        if(highFrequencyVehicles.contains(vehicleId) || highFrequencyVehicles.contains("*")){
            Date vehicleTimestamp = getVehicleTimestamp(ccLocationReport);
            processMessage(vehicleId, vehicleTimestamp, message.toString());
        } else {
            publisher.send(message.toString());
        }
    }

    private String getRouteId(JsonNode ccLocationReport) {
         return ccLocationReport
                 .get("routeID")
                 .get("route-designator")
                 .textValue();
    }

    private String getVehicleId(JsonNode ccLocationReport) {
        return ccLocationReport
                .get("vehicle")
                .get("vehicle-id")
                .asText();
    }

    private Date getVehicleTimestamp(JsonNode ccLocationReport) throws ParseException {
        String timeReported = ccLocationReport
                .get("time-reported")
                .textValue();
        return parseDate(timeReported);
    }

    private void processMessage(String vehicleId, Date vehicleTimestamp, String message) throws ExecutionException {
        highFreqPublisher.send(message);

        vehicleTimestamp = getFixedVehicleTimestamp(vehicleTimestamp);

        if(shouldProcessVehicle(vehicleId, vehicleTimestamp)){
            _log.debug("Adding {} with timestamp {} to the cache", vehicleId, vehicleTimestamp);
            lastKnownVehicleRecords.put(vehicleId, vehicleTimestamp);
            publisher.send(message);
        }
    }

    private boolean shouldProcessVehicle(String vehicleId, Date currentVehicleTimestamp){
        Date lastVehicleTime = lastKnownVehicleRecords.get(vehicleId);
        if(lastVehicleTime == null) {
            return true;
        }

        long timeSinceLastUpdateMillis = currentVehicleTimestamp.getTime() - lastVehicleTime.getTime();

        // Checks for vehicles that are more than 25 seconds old OR vehicles that have the same timestamp
        if(TimeUnit.MILLISECONDS.toSeconds(timeSinceLastUpdateMillis) > 25 || timeSinceLastUpdateMillis == 0){
            return true;
        }
        return false;
    }

    private Date getFixedVehicleTimestamp(Date vehicleTimestamp){
        if(vehicleTimestamp.getTime() > System.currentTimeMillis()){
            return new Date();
        }
        return vehicleTimestamp;
    }



    // DNS CHECK - Reloads queue if DNS information changes
    public void startDNSCheckThread() {
        String host = getHost();
        _log.info("listening on interface " + host);
        _resolver = new DNSResolver(host);

        if (_taskScheduler != null) {
            DNSCheckThread dnsCheckThread = new DNSCheckThread();
            // Check every 20 seconds
            _taskScheduler.scheduleWithFixedDelay(dnsCheckThread, TimeUnit.SECONDS.toMillis(20));
        }
    }

    private String getHost() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (Exception e) {
            _log.error("getHost Exception:", e);
        }
        return "localhost";
    }

    private class DNSCheckThread extends TimerTask {
        @Override
        public void run() {
            try {
                if (_resolver.hasAddressChanged()) {
                    _log.warn("Resolver Changed -- re-binding queue connection");
                    publisher.reset();
                    highFreqPublisher.reset();
                }
            } catch (Exception e) {
                _log.error(e.toString());
                _resolver.reset();
            }
        }
    }




}
