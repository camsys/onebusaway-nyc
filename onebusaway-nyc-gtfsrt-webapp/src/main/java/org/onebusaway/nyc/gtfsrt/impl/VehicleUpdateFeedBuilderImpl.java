package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.realtime.api.VehicleLocationRecord;

public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {
    public GtfsRealtime.VehiclePosition getVehicleUpdateFromInferredLocation(VehicleLocationRecord record) {
        return GtfsRealtime.VehiclePosition.newBuilder()
                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId(record.getVehicleId().getId()))
                .setPosition(GtfsRealtime.Position.newBuilder()
                        .setLatitude((float) record.getCurrentLocationLat())
                        .setLongitude((float) record.getCurrentLocationLon()))
                .build();
    }
}
