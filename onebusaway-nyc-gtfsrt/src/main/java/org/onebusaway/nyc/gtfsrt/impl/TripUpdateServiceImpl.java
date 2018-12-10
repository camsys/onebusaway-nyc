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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Build TripUpdate feed for current time.
 */
@Component
public class TripUpdateServiceImpl extends AbstractFeedMessageService {

    private TripUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;
    private PresentationService _presentationService;


    private static final Logger _log = LoggerFactory.getLogger(TripUpdateServiceImpl.class);

    @Autowired
    public void setFeedBuilder(TripUpdateFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Autowired
    public void setPresentationService(PresentationService presentationService) {
        _presentationService = presentationService;
    }

    private Cache<String, BlockInstanceBean> _blockCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    private BlockInstanceBean getBlock(VehicleStatusBean vehicle) {
        String blockId = vehicle.getTrip().getBlockId();
        long serviceDate = vehicle.getTripStatus().getServiceDate();
        String key = blockId + " " + Long.toString(serviceDate);
        BlockInstanceBean block = _blockCache.getIfPresent(key);
        if (block == null) {
            block =  _transitDataService.getBlockInstance(blockId, serviceDate);
            _blockCache.put(key, block);
        }
        return block;
    }

    @Override
    public List<FeedEntity.Builder> getEntities(long time) {
        _log.debug("getEntities(" + new Date(time) + ")");

        Collection<VehicleStatusBean> vehicles = getAllVehicles(_transitDataService, _presentationService, time);
        _log.debug("found " + vehicles.size() + " vehicles");
        List<FeedEntity.Builder> entities = new ArrayList<FeedEntity.Builder>();

        for (VehicleStatusBean vehicle : vehicles) {

            if (vehicle.getTrip() == null) {
                _log.warn("no trip for vehicle=" + vehicle.getVehicleId());
                continue;
            }

            int tripSequence = vehicle.getTripStatus().getBlockTripSequence();
            BlockInstanceBean block = getBlock(vehicle);
            List<BlockTripBean> trips = block.getBlockConfiguration().getTrips();

            for (int i = tripSequence; i < trips.size(); i++) {
                TripBean trip = trips.get(i).getTrip();
                List<TimepointPredictionRecord> tprs = _transitDataService.getPredictionRecordsForVehicleAndTrip(vehicle.getVehicleId(), trip.getId());
                if (tprs == null || tprs.isEmpty()) {
                    _log.debug("no tprs for time=" + new Date(time));
                    break;
                }

                GtfsRealtime.TripUpdate.Builder tu  = _feedBuilder.makeTripUpdate(trip, vehicle, tprs);

                FeedEntity.Builder entity = FeedEntity.newBuilder();
                entity.setTripUpdate(tu);
                entity.setId(tu.getTrip().getTripId());
                entities.add(entity);
            }

        }
        _log.debug("returning " + entities.size() + " entities");
        return entities;
    }

}
