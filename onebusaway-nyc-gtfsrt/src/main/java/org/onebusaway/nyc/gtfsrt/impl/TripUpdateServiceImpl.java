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
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Qualifier("NycPresentationService")
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

        List<CancelledTripBean> allCancelledTrips = getAllCanceledTrips();

        Collection<VehicleStatusBean> vehicles = getAllVehicles(_transitDataService, _presentationService, time);
        Set<String> cancelledTripIds = getCanceledTripIds(allCancelledTrips);
        _log.debug("found " + vehicles.size() + " vehicles");
        List<FeedEntity.Builder> entities = new ArrayList<FeedEntity.Builder>();

        for (VehicleStatusBean vehicle : vehicles) {
            int tripSequence = vehicle.getTripStatus().getBlockTripSequence();
            BlockInstanceBean block = getBlock(vehicle);
            List<BlockTripBean> trips = block.getBlockConfiguration().getTrips();

            for (int i = tripSequence; i < trips.size(); i++) {
                TripBean trip = trips.get(i).getTrip();

                if (cancelledTripIds.contains(trip.getId())) {
                    // CAPI contradicts this data, drop it from feed
                    _log.warn("suppressing trip " + trip.getId() + " as its in CAPI");
                } else {
                    List<TimepointPredictionRecord> tprs = _transitDataService.getPredictionRecordsForVehicleAndTrip(vehicle.getVehicleId(), trip.getId());
                    boolean isSpooking = vehicle != null && vehicle.getPhase() != null && !vehicle.getPhase().equalsIgnoreCase(EVehiclePhase.SPOOKING.toLabel());
                    if ((tprs == null || tprs.isEmpty()) && !isSpooking){
                        _log.debug("no tprs for time=" + new Date(time));
                        break;
                    }

                    GtfsRealtime.TripUpdate.Builder tu = _feedBuilder.makeTripUpdate(trip, vehicle, tprs);

                    FeedEntity.Builder entity = FeedEntity.newBuilder();
                    entity.setTripUpdate(tu);
                    entity.setId(tu.getTrip().getTripId());
                    entities.add(entity);
                }
            }

        }

        for (CancelledTripBean bean : allCancelledTrips) {
            // we have a cancelled trip, ignore any vehicle associated with it
            // and send schedule relationship CANCELED
            TripBean tripBean = getBeanForTrip(bean.getTrip());
            if (tripBean != null) {
                GtfsRealtime.TripUpdate.Builder tu = _feedBuilder.makeCanceledTrip(tripBean);
                FeedEntity.Builder entity = FeedEntity.newBuilder();
                entity.setTripUpdate(tu);
                entity.setId(tu.getTrip().getTripId());
                entities.add(entity);
            } else {
                _log.warn("no trip for id " + bean.getTrip());
            }
        }

        _log.debug("returning " + entities.size() + " entities");
        return entities;
    }

    private List<CancelledTripBean> getAllCanceledTrips() {
        ListBean<CancelledTripBean> listBean = _transitDataService.getAllCancelledTrips();
        if (listBean != null) {
            return listBean.getList();
        }
        return Collections.emptyList();
    }

    private TripBean getBeanForTrip(String tripId) {
        return _transitDataService.getTrip(tripId);
    }

    private Set<String> getCanceledTripIds(List<CancelledTripBean> allCancelledTrips) {
        if (allCancelledTrips == null) return Collections.emptySet();
        Set<String> canceled = new HashSet<>();
        for (CancelledTripBean bean : allCancelledTrips) {
            canceled.add(bean.getTrip());
        }
        return canceled;
    }


}
