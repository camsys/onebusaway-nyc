package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class TripUpdateFeedBuilderImpl implements TripUpdateFeedBuilder {

    private static final Logger _log = LoggerFactory.getLogger(TripUpdateFeedBuilderImpl.class);

    private NycTransitDataService _transitDataService;

    private ScheduledBlockLocationService _scheduledBlockLocationService;

    private BlockCalendarService _blockCalendarService;

    private TransitGraphDao _transitGraphDao;

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

    @Autowired
    public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
        _transitGraphDao = transitGraphDao;
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

        TripEntry tripEntry = _transitGraphDao.getTripEntryForId(AgencyAndId.convertFromString(trip.getId()));
        // It is probably true that tripEntry.getStopTimes().get(i).getSequence() = i, but perhaps not,
        // because of StopTimeEntriesFactory.removeDuplicateStopTimes()
        Map<Integer, Integer> stopSequenceToGtfsSequence = getSequenceMap(tripEntry);

        for (TimepointPredictionRecord tpr : records) {
            int stopSequence = tpr.getStopSequence();
            int gtfsSequence = -1;
            if (stopSequenceToGtfsSequence.get(stopSequence) != null)
                gtfsSequence = stopSequenceToGtfsSequence.get(stopSequence);
            tripUpdate.addStopTimeUpdate(makeStopTimeUpdate(tpr, gtfsSequence));
        }

        return tripUpdate;
    }

    private Map<Integer, Integer> getSequenceMap(TripEntry tripEntry) {
        Map<Integer, Integer> sequenceMap = new HashMap<Integer, Integer>();
        if (tripEntry != null) {
            for (StopTimeEntry stopTime : tripEntry.getStopTimes()) {
                sequenceMap.put(stopTime.getSequence(), stopTime.getGtfsSequence());
            }
        }
        return sequenceMap;
    }

    // see appmods GtfsRealtimeTripLibrary:870
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
