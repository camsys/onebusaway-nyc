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

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.*;
import com.google.transit.realtime.GtfsRealtimeOneBusAway;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class  TripUpdateFeedBuilderImpl implements TripUpdateFeedBuilder {

    private static final Logger _log = LoggerFactory.getLogger(TripUpdateFeedBuilderImpl.class);

    private NycTransitDataService _transitDataService;

    private ScheduledBlockLocationService _scheduledBlockLocationService;

    private BlockCalendarService _blockCalendarService;

    /**
     * true if TripUpdate delay should be set based on the effective schedule time (calculated as
     * the difference between the current time and the linear-interpolated schedule time).
     * false if delay should be based on scheduleAdherence, which may take into account
     * travel time estimates (default).
     */
    private boolean _useEffectiveScheduleTime = false;

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Autowired
    public void setScheduledBlockLocationService(ScheduledBlockLocationService scheduledBlockLocationService) {
        _scheduledBlockLocationService = scheduledBlockLocationService;
    }

    @Autowired
    public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
        _blockCalendarService = blockCalendarService;
    }

    public void setUseEffectiveScheduleTime(boolean useEffectiveScheduleTime) {
        _useEffectiveScheduleTime = useEffectiveScheduleTime;
    }

    @Override
    public TripUpdate.Builder makeTripUpdate(TripBean trip, VehicleStatusBean vehicle, List<TimepointPredictionRecord> records) {
        TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();

        tripUpdate.setTrip(makeTripDescriptor(trip, vehicle));
        tripUpdate.setVehicle(makeVehicleDescriptor(vehicle));
        tripUpdate.setTimestamp(vehicle.getLastUpdateTime()/1000);

        double delay = useEffectiveScheduleTime() ? getEffectiveScheduleTime(trip, vehicle) : vehicle.getTripStatus().getScheduleDeviation();
        tripUpdate.setDelay((int) delay);

        if (EVehiclePhase.SPOOKING.equals(EVehiclePhase.valueOf(vehicle.getPhase().toUpperCase()))) {
            GtfsRealtimeOneBusAway.OneBusAwayTripUpdate.Builder obaTripUpdate =
                    GtfsRealtimeOneBusAway.OneBusAwayTripUpdate.newBuilder();
            obaTripUpdate.setIsEstimatedRealtime(true);
            tripUpdate.setExtension(GtfsRealtimeOneBusAway.obaTripUpdate, obaTripUpdate.build());
            // we don't make predictions if its SPOOKING
            return tripUpdate;
        }
        for (TimepointPredictionRecord tpr : records) {
            tripUpdate.addStopTimeUpdate(makeStopTimeUpdate(tpr));
        }

        return tripUpdate;
    }

    @Override
    public GtfsRealtime.TripUpdate.Builder makeCanceledTrip(TripBean trip) {
        TripStatusBean empty = new TripStatusBean();
        empty.setServiceDate(System.currentTimeMillis());
        return GtfsRealtimeLibrary.makeCanceledTrip(trip, empty);
    }

    // see appmods GtfsRealtimeTripLibrary:870

    /**
     * Calculate effective schedule time for a trip and vehicle status.
     *
     * Effective schedule time is the difference between the time of the record and the linear-interpolated
     * schedule time.
     *
     * @param trip the trip
     * @param vehicle the vehicle
     * @return effective schedule time
     */
    private double getEffectiveScheduleTime(TripBean trip, VehicleStatusBean vehicle) {
        double deviation = vehicle.getTripStatus().getScheduleDeviation();
        long serviceDate = vehicle.getTripStatus().getServiceDate();
        long timestamp = vehicle.getLastUpdateTime();

        BlockInstance blockInstance = _blockCalendarService.getBlockInstance(AgencyAndId.convertFromString(trip.getBlockId()), serviceDate);
        VehicleLocationRecordBean vlrb = _transitDataService.getVehicleLocationRecordForVehicleId(vehicle.getVehicleId(), timestamp);
        if (vlrb == null) {
            _log.info("no vlrb for vehicle={}", vehicle.getVehicleId());
            return deviation;
        }

        ScheduledBlockLocation loc = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
                blockInstance.getBlock(),
                vlrb.getDistanceAlongBlock());

        if (loc == null) {
            _log.info("no location for vehicle={}, distance along block={}", vehicle.getVehicleId(), vlrb.getDistanceAlongBlock());
            return deviation;
        }

        long effectiveScheduleTime = loc.getScheduledTime() + (serviceDate/1000);
        double efftime = timestamp/1000 - effectiveScheduleTime;
        _log.info("vehicle={}, effsched={}, deviation={}", new Object[] { vehicle.getVehicleId(), efftime, deviation });
        return efftime;
    }

    private boolean useEffectiveScheduleTime() {
        return _useEffectiveScheduleTime && _transitDataService != null && _blockCalendarService != null && _scheduledBlockLocationService != null;
    }

}
