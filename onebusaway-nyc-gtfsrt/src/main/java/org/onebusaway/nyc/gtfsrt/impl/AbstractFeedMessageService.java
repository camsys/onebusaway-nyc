/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Includes shared logic for FeedMessageServices. Maintains a background thread
 * that periodically refreshes a cached FeedMessage. Callers receive the last
 * successfully built message immediately without blocking on data retrieval.
 */
public abstract class AbstractFeedMessageService implements FeedMessageService {

    private static final Logger _log = LoggerFactory.getLogger(AbstractFeedMessageService.class);

    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 10;

    private int _refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SECONDS;

    private volatile FeedMessage _cachedMessage = null;

    private ScheduledExecutorService _scheduler;

    public void setRefreshIntervalSeconds(int seconds) {
        _refreshIntervalSeconds = seconds;
    }

    @PostConstruct
    public void init() {
        _scheduler = Executors.newSingleThreadScheduledExecutor();
        _scheduler.scheduleWithFixedDelay(this::refresh, 0, _refreshIntervalSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (_scheduler != null) {
            _scheduler.shutdownNow();
        }
    }

    /**
     * Returns the last successfully cached FeedMessage, or an empty feed if
     * no successful refresh has completed yet.
     */
    @Override
    public FeedMessage getFeedMessage() {
        FeedMessage cached = _cachedMessage;
        if (cached == null) {
            return buildEmptyFeedMessage();
        }
        return cached;
    }

    /**
     * If a specific time is provided (debug/replay use), builds the feed
     * in real-time at that timestamp. Otherwise returns the cached message.
     */
    @Override
    public FeedMessage getFeedMessage(Long requestTime) {
        if (requestTime != null && requestTime > 0) {
            return buildFeedMessage(requestTime);
        }
        return getFeedMessage();
    }

    /**
     * Called by the background thread on each refresh cycle. Builds the feed
     * and updates the cache only if the result is non-null.
     */
    private void refresh() {
        try {
            List<FeedEntity.Builder> entities = getEntities(System.currentTimeMillis());
            if (entities == null) {
                _log.warn("{}: getEntities returned null, skipping cache update", getClass().getSimpleName());
                return;
            }
            FeedMessage message = buildFeedMessage(entities, System.currentTimeMillis());
            _cachedMessage = message;
        } catch (Exception e) {
            _log.error("{}: error during feed refresh, retaining previous cache", getClass().getSimpleName(), e);
        }
    }

    private FeedMessage buildFeedMessage(long time) {
        List<FeedEntity.Builder> entities = getEntities(time);
        if (entities == null) {
            return buildEmptyFeedMessage();
        }
        return buildFeedMessage(entities, time);
    }

    private FeedMessage buildFeedMessage(List<FeedEntity.Builder> entities, long time) {
        FeedMessage.Builder builder = FeedMessage.newBuilder();
        for (FeedEntity.Builder entity : entities) {
            if (entity != null) {
                try {
                    builder.addEntity(entity);
                } catch (Exception ex) {
                    _log.error("Unable to process entity {}", entity.getId(), ex);
                }
            }
        }
        FeedHeader.Builder header = FeedHeader.newBuilder();
        header.setGtfsRealtimeVersion("1.0");
        header.setTimestamp(time / 1000);
        header.setIncrementality(FeedHeader.Incrementality.FULL_DATASET);
        builder.setHeader(header);
        return builder.build();
    }

    private FeedMessage buildEmptyFeedMessage() {
        FeedMessage.Builder builder = FeedMessage.newBuilder();
        FeedHeader.Builder header = FeedHeader.newBuilder();
        header.setGtfsRealtimeVersion("1.0");
        header.setTimestamp(System.currentTimeMillis() / 1000);
        header.setIncrementality(FeedHeader.Incrementality.FULL_DATASET);
        builder.setHeader(header);
        return builder.build();
    }

    public Collection<VehicleStatusBean> getAllVehicles(TransitDataService tds, PresentationService ps, long time) {
        List<VehicleStatusBean> vehicles = new ArrayList<VehicleStatusBean>();
        for (AgencyWithCoverageBean bean : tds.getAgenciesWithCoverage()) {
            String agency = bean.getAgency().getId();
            ListBean<VehicleStatusBean> lb = tds.getAllVehiclesForAgency(agency, time);
            for (VehicleStatusBean vsb : lb.getList()) {
                if (includeVehicle(tds, ps, vsb, time)) {
                    vehicles.add(vsb);
                }
            }
        }
        return vehicles;
    }

    public boolean includeVehicle(TransitDataService tds, PresentationService presentationService,
                                  VehicleStatusBean vehicleStatus, long time) {
        TripDetailsBean tripDetails = getTripForVehicle(tds, vehicleStatus, time);
        presentationService.setTime(time);
        if (tripDetails == null || !presentationService.include(tripDetails.getStatus()) || vehicleStatus.getTrip() == null) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private TripDetailsBean getTripForVehicle(TransitDataService tds, VehicleStatusBean vehicleStatus, long time) {
        TripForVehicleQueryBean query = new TripForVehicleQueryBean();
        query.setTime(new Date(time));
        query.setVehicleId(vehicleStatus.getVehicleId());

        TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
        inclusion.setIncludeTripStatus(true);
        inclusion.setIncludeTripBean(true);
        query.setInclusion(inclusion);

        return tds.getTripDetailsForVehicleAndTime(query);
    }

    public abstract List<FeedEntity.Builder> getEntities(long time);
}
