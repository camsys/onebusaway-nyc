package org.onebusaway.nyc.gtfsrt.util;

import com.google.transit.realtime.GtfsRealtime.*;
import org.joda.time.LocalDate;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public class GtfsRealtimeLibrary {
    public static TripDescriptor.Builder makeTripDescriptor(TripBean tb, TripStatusBean status) {
        TripDescriptor.Builder trip = TripDescriptor.newBuilder();
        trip.setTripId(tb.getId());
        trip.setRouteId(tb.getRoute().getId());

        // start date - YYYYMMDD
        LocalDate ld = new LocalDate(status.getServiceDate());
        trip.setStartDate(ld.toString("yyyyMMdd"));

        return trip;
    }

    public static TripDescriptor.Builder makeTripDescriptor(TripBean tb, VehicleStatusBean vehicle) {
        return makeTripDescriptor(tb, vehicle.getTripStatus());
    }

    public static TripDescriptor.Builder makeTripDescriptor(VehicleStatusBean vehicle) {
        return makeTripDescriptor(vehicle.getTrip(), vehicle.getTripStatus());
    }

    public static Position.Builder makePosition(VehicleLocationRecordBean record) {
        Position.Builder pos = Position.newBuilder();
        pos.setLatitude((float) record.getCurrentLocation().getLat());
        pos.setLongitude((float) record.getCurrentLocation().getLon());
        pos.setBearing((float) record.getCurrentOrientation());
        return pos;
    }

    public static VehicleDescriptor.Builder makeVehicleDescriptor(VehicleStatusBean vsb) {
        VehicleDescriptor.Builder vehicle = VehicleDescriptor.newBuilder();
        vehicle.setId(vsb.getVehicleId());
        return vehicle;
    }

    public static VehicleDescriptor.Builder makeVehicleDescriptor(VehicleLocationRecordBean vlrb) {
        VehicleDescriptor.Builder vehicle = VehicleDescriptor.newBuilder();
        vehicle.setId(vlrb.getVehicleId());
        return vehicle;
    }

    public static TripUpdate.StopTimeUpdate.Builder makeStopTimeUpdate(TimepointPredictionRecord tpr) {
        TripUpdate.StopTimeUpdate.Builder builder = TripUpdate.StopTimeUpdate.newBuilder();
        builder.setStopId(tpr.getTimepointId().toString());
        builder.setArrival(makeStopTimeEvent(tpr.getTimepointPredictedTime()/1000));
        builder.setDeparture(makeStopTimeEvent(tpr.getTimepointPredictedTime()/1000)); // TODO: different?
        return builder;
    }

    public static TripUpdate.StopTimeEvent.Builder makeStopTimeEvent(long time) {
        return TripUpdate.StopTimeEvent.newBuilder()
                .setTime(time);
    }
}
