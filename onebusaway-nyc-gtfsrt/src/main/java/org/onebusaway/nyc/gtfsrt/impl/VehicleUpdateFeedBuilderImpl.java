package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.springframework.stereotype.Component;


import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {

    @Override
    public VehiclePosition.Builder makeVehicleUpdate(VehicleStatusBean status, VehicleLocationRecordBean record) {
        VehiclePosition.Builder position = VehiclePosition.newBuilder();
        position.setTrip(makeTripDescriptor(status));
        position.setPosition(makePosition(record));
        position.setVehicle(makeVehicleDescriptor(record));
        position.setTimestamp(record.getTimeOfRecord()/1000);
        if (status.getTripStatus().getNextStop() != null)
            position.setStopId(status.getTripStatus().getNextStop().getId());

        return position;
    }

}
