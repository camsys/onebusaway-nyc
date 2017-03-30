package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class TripUpdateFeedBuilderImpl implements TripUpdateFeedBuilder {


    @Override
    public TripUpdate.Builder makeTripUpdate(TripBean trip, VehicleStatusBean vehicle, List<TimepointPredictionRecord> records) {
        TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();

        tripUpdate.setTrip(makeTripDescriptor(trip, vehicle));
        tripUpdate.setVehicle(makeVehicleDescriptor(vehicle));

        tripUpdate.setTimestamp(vehicle.getLastUpdateTime()/1000);
        tripUpdate.setDelay((int)vehicle.getTripStatus().getScheduleDeviation());

        for (TimepointPredictionRecord tpr : records) {
            tripUpdate.addStopTimeUpdate(makeStopTimeUpdate(tpr));
        }

        return tripUpdate;
    }


}
