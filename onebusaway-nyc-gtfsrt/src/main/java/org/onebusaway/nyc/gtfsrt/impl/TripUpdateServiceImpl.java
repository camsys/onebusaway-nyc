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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trip_mods.StopTimeSnapshot;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Build TripUpdate feed for current time.
 */
@Component
public class TripUpdateServiceImpl extends AbstractFeedMessageService {

    private TripUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;
    private PresentationService _presentationService;
    private final static String MODIFICATION_PREFIX = "modification_";


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
            boolean isSpookingVehicle = vehicle != null
                    && vehicle.getPhase() != null
                    && vehicle.getPhase().equalsIgnoreCase(EVehiclePhase.SPOOKING.toLabel());

            if(isSpookingVehicle){
                addSpookingEntity(entities, vehicle);
            } else{
                addPredictedEntities(entities, vehicle, cancelledTripIds, time);
            }
        }

        addCanceledTripEntities(entities, allCancelledTrips);

        _log.debug("returning " + entities.size() + " entities");
        return entities;
    }

    private void addCanceledTripEntities(List<FeedEntity.Builder> entities, List<CancelledTripBean> allCancelledTrips) {
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
    }

    private void addPredictedEntities(List<FeedEntity.Builder> entities, VehicleStatusBean vehicle, Set<String> cancelledTripIds, long time) {
        int tripSequence = vehicle.getTripStatus().getBlockTripSequence();
        BlockInstanceBean block = getBlock(vehicle);
        List<BlockTripBean> trips = block.getBlockConfiguration().getTrips();
        Map<AgencyAndId, TripModificationDiff> tripModificationDiffs = null;
        String timeZone = block.getBlockConfiguration().getTimeZone();
        try {
            LocalDate serviceDate = getServiceLocalDate(block.getServiceDate(), timeZone);
            tripModificationDiffs = _transitDataService.getAllTripModificationDiffsById(serviceDate);
       }
        catch(Exception e){
            _log.error("Error retrieving trip modification diffs", e);
        }

        for (int i = tripSequence; i < trips.size(); i++) {
            BlockTripBean blockTripBean = trips.get(i);
            TripBean trip = blockTripBean.getTrip();
            String tripId = trip.getId();

            // Skip if cancelled trip
            if (cancelledTripIds.contains(trip.getId())) {
                // CAPI contradicts this data, drop it from feed
                _log.warn("suppressing trip " + trip.getId() + " as its in CAPI");
                continue;
            }

            Map<String, TimepointPredictionRecord> timepointPredictionRecords =
                    _transitDataService.getPredictionRecordsForVehicleAndTripByStopId(vehicle.getVehicleId(), trip.getId());

            // If no time point record found then its likely that all future trips won't have time point records
            // therefore we break the loop here
            if ((timepointPredictionRecords == null || timepointPredictionRecords.isEmpty())){
                _log.debug("no tprs for time=" + new Date(time));
                break;
            }

            TripModificationDiff tripModificationDiff = tripModificationDiffs.get(AgencyAndId.convertFromString(tripId));
            boolean hasTripModification = tripModificationDiff != null;

            GtfsRealtime.TripUpdate.Builder tu = _feedBuilder.makeTripUpdate(trip, vehicle, timepointPredictionRecords.values(),
                    tripModificationDiff);

            String entityId = tripId;
            if (hasTripModification) {
                entityId = MODIFICATION_PREFIX + tripId;
            }

            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setTripUpdate(tu);
            entity.setId(entityId);
            entities.add(entity);

            // Show original Trip TripUpdate
            if(hasTripModification){
                Map<AgencyAndId, StopTimeSnapshot> originalTripStopTimes = tripModificationDiff.getOriginalStopTimes();

                Collection<TimepointPredictionRecord> originalTripTimepointRecords = reconstructOriginalTripTimepointRecords(
                        originalTripStopTimes.values(), timepointPredictionRecords, serviceDate, timeZone);

                GtfsRealtime.TripUpdate.Builder originalTripUpdate = _feedBuilder.makeTripUpdate(trip, vehicle,
                        originalTripTimepointRecords, null);

                FeedEntity.Builder originalTripEntity = FeedEntity.newBuilder();
                originalTripEntity.setTripUpdate(originalTripUpdate);
                originalTripEntity.setId(tripId);
                entities.add(originalTripEntity);
            }
        }
    }

    private Collection<TimepointPredictionRecord> reconstructOriginalTripTimepointRecords(
            Collection<StopTimeSnapshot> originalTripStopTimes,
            Map<String, TimepointPredictionRecord> timepointPredictionRecords,
            LocalDate serviceDate,
            String timeZone) {

        Collection<TimepointPredictionRecord> originalTripTimepointRecords = new ArrayList<>();
        long secondsSinceStartOfServiceDate = calculateSecondsSinceStartOfServiceDate(serviceDate, timeZone);

        for(StopTimeSnapshot originalStopTime : originalTripStopTimes){
            TimepointPredictionRecord record = timepointPredictionRecords.get(originalStopTime.getStopId());
            if(record != null){
                originalTripTimepointRecords.add(makeOriginalTripRecord(originalStopTime, record));
            } else if(scheduledArrivalTimeIsNotInPast(originalStopTime,secondsSinceStartOfServiceDate)){
                originalTripTimepointRecords.add(makeSkippedRecord(originalStopTime));
            }
        }

        return originalTripTimepointRecords;
    }

    private long calculateSecondsSinceStartOfServiceDate(LocalDate serviceDate,
                                                  String timeZone) {
        ZoneId agencyZone = ZoneId.of(timeZone);
        ZonedDateTime serviceDayMidnight = serviceDate.atStartOfDay(agencyZone);
        ZonedDateTime now = ZonedDateTime.now(agencyZone);

        return ChronoUnit.SECONDS.between(serviceDayMidnight, now);
    }

    private boolean scheduledArrivalTimeIsNotInPast(StopTimeSnapshot stopTime,
                                                    long secondsSinceStartOfServiceDate) {
        int bufferSeconds = 120;
        return stopTime.getArrivalTime() + bufferSeconds >= secondsSinceStartOfServiceDate
        || stopTime.getDepartureTime() + bufferSeconds >= secondsSinceStartOfServiceDate;
    }

    private TimepointPredictionRecord makeOriginalTripRecord(StopTimeSnapshot originalStopTime,
                                                             TimepointPredictionRecord record) {

        TimepointPredictionRecord originalStopRecord = new TimepointPredictionRecord();
        originalStopRecord.setTimepointId(record.getTimepointId());
        originalStopRecord.setStopSequence(originalStopTime.getGtfsSequence());
        if(record.getScheduleRelationship() != null){
            originalStopRecord.setScheduleRealtionship(record.getScheduleRelationship().getValue());
        }
        originalStopRecord.setTripId(record.getTripId());
        originalStopRecord.setTimepointPredictedArrivalTime(record.getTimepointPredictedArrivalTime());
        originalStopRecord.setTimepointPredictedDepartureTime(record.getTimepointPredictedDepartureTime());

        return originalStopRecord;
    }


    private TimepointPredictionRecord makeSkippedRecord(StopTimeSnapshot stopTime) {
        TimepointPredictionRecord record = new TimepointPredictionRecord();
        record.setTimepointId(AgencyAndId.convertFromString(stopTime.getStopId()));
        record.setStopSequence(stopTime.getGtfsSequence());
        record.setScheduleRealtionship(
                TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue());
        return record;
    }

    private void addSpookingEntity(List<FeedEntity.Builder> entities, VehicleStatusBean vehicle) {
        boolean hasActiveTrip = vehicle != null
                && vehicle.getTripStatus() != null
                && vehicle.getTripStatus().getActiveTrip() != null;

        if(hasActiveTrip){
            BlockInstanceBean block = getBlock(vehicle);
            LocalDate serviceDate = getServiceLocalDate(block.getServiceDate(), block.getBlockConfiguration().getTimeZone());
            TripBean trip = vehicle.getTripStatus().getActiveTrip();

            Map<AgencyAndId, TripModificationDiff> tripModificationDiffs = _transitDataService.getAllTripModificationDiffsById(serviceDate);
            TripModificationDiff tripModificationDiff = tripModificationDiffs.get(AgencyAndId.convertFromString(trip.getId()));

            GtfsRealtime.TripUpdate.Builder tu = _feedBuilder.makeTripUpdate(trip, vehicle, Collections.emptyList(), tripModificationDiff);

            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setTripUpdate(tu);
            entity.setId(tu.getTrip().getTripId());
            entities.add(entity);
        }
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

    private LocalDate getServiceLocalDate(long serviceDate, String timeZone){
        return Instant.ofEpochMilli(serviceDate).atZone(ZoneId.of(timeZone)).toLocalDate();
    }


}
