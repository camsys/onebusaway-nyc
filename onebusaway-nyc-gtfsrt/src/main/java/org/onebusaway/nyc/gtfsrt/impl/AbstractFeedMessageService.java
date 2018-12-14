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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Includes shared logic for FeedMessageServices. Builds the FeedMessage given a list of FeedEntities.
 */
public abstract class AbstractFeedMessageService implements FeedMessageService {

    public static final int DEFAULT_CACHE_EXPIRY_SECONDS = 10;

    private long _generatedTime = 0l;
    private int _cacheExpirySeconds = DEFAULT_CACHE_EXPIRY_SECONDS;
    private FeedMessage _cache = null;
    private boolean _disableCache = false;

    public void setCacheExpirySeconds(int seconds) {
        _cacheExpirySeconds = seconds;
    }

    public void setDisableCache(boolean disableCache) {
        _disableCache = disableCache;
    }

    @Override
    public synchronized FeedMessage getFeedMessage() {
        return getFeedMessage(null);
    }
        @Override
    public synchronized FeedMessage getFeedMessage(Long requestTime) {
        long time = System.currentTimeMillis();
        if (requestTime != null && requestTime > 0)
            time = requestTime;

        if (_disableCache || isCacheExpired(time)) {
            FeedMessage.Builder builder = FeedMessage.newBuilder();

            for (FeedEntity.Builder entity : getEntities(time))
                if (entity != null)
                    builder.addEntity(entity);

            FeedHeader.Builder header = FeedHeader.newBuilder();
            header.setGtfsRealtimeVersion("1.0");
            long headerTime = requestTime != null ? requestTime : System.currentTimeMillis();
            header.setTimestamp(headerTime / 1000);
            header.setIncrementality(FeedHeader.Incrementality.FULL_DATASET);
            builder.setHeader(header);
            FeedMessage message = builder.build();
            _cache = message;
            _generatedTime = time;
        }
        return _cache;
    }

    private boolean isCacheExpired(long now) {
        if (_cache == null) return true;
        return (now - _generatedTime) > _cacheExpirySeconds * 1000;
    }

    public Collection<VehicleStatusBean> getAllVehicles(TransitDataService tds, PresentationService ps, long time) {
        List<VehicleStatusBean> vehicles = new ArrayList<VehicleStatusBean>();
        for (AgencyWithCoverageBean bean : tds.getAgenciesWithCoverage()) {
            String agency = bean.getAgency().getId();
            ListBean<VehicleStatusBean> lb = tds.getAllVehiclesForAgency(agency, time);
            for(VehicleStatusBean vsb : lb.getList()){
                if(includeVehicle(tds,ps,vsb,time)){
                    vehicles.add(vsb);
                }
            }
        }
        return vehicles;
    }

    public boolean includeVehicle(TransitDataService tds, PresentationService presentationService,
                                  VehicleStatusBean vehicleStatus, long time){
        TripDetailsBean tripDetails = getTripForVehicle(tds, vehicleStatus, time);
        presentationService.setTime(time);
        if (tripDetails == null || !presentationService.include(tripDetails.getStatus())){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private TripDetailsBean getTripForVehicle(TransitDataService tds, VehicleStatusBean vehicleStatus, long time){
        TripForVehicleQueryBean query = new TripForVehicleQueryBean();
        query.setTime(new Date(time));
        query.setVehicleId(vehicleStatus.getVehicleId());

        TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
        inclusion.setIncludeTripStatus(true);
        inclusion.setIncludeTripBean(true);
        query.setInclusion(inclusion);

        TripDetailsBean tripDetailsForCurrentTrip = tds.getTripDetailsForVehicleAndTime(query);

        return tripDetailsForCurrentTrip;
    }

    public abstract List<FeedEntity.Builder> getEntities(long time);
}
