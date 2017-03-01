package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.TripDetailsService;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.springframework.beans.factory.annotation.Autowired;

public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {

    private TripDetailsService _tripDetailsService;

    @Autowired
    public void setTripDetailsService(TripDetailsService tripDetailsService) {
        _tripDetailsService = tripDetailsService;
    }

    @Override
    public VehiclePosition getVehicleUpdateFromInferredLocation(VehicleLocationRecordBean record) {
        VehiclePosition.Builder position = VehiclePosition.newBuilder();

        TripDetailsBean td = _tripDetailsService.getTripDetailsForVehicleLocationRecord(record);
        position.setTrip(makeTripDescriptor(td));
        position.setPosition(makePosition(record));
        position.setVehicle(makeVehicleDescriptor(record));
        position.setTimestamp(record.getTimeOfRecord()/1000);
        position.setStopId(td.getStatus().getNextStop().getId());

        return position.build();
    }

    private TripDescriptor.Builder makeTripDescriptor(TripDetailsBean td) {
        TripDescriptor.Builder trip = TripDescriptor.newBuilder();
        trip.setTripId(td.getTripId());
        trip.setRouteId(td.getTrip().getRoute().getId());
        return trip;
    }

    private Position.Builder makePosition(VehicleLocationRecordBean record) {
        Position.Builder pos = Position.newBuilder();
        pos.setLatitude((float) record.getCurrentLocation().getLat());
        pos.setLongitude((float) record.getCurrentLocation().getLon());
        pos.setBearing((float) record.getCurrentOrientation());
        return pos;
    }

    private VehicleDescriptor.Builder makeVehicleDescriptor(VehicleLocationRecordBean record) {
        VehicleDescriptor.Builder vehicle = VehicleDescriptor.newBuilder();
        vehicle.setId(record.getVehicleId());
        return vehicle;
    }
}
