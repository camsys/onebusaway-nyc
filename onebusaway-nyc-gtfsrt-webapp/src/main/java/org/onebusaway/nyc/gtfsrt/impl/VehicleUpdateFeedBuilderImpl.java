package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.springframework.stereotype.Component;


import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {

    @Override
    public VehiclePosition.Builder makeVehicleUpdate(VehicleLocationRecordBean record, TripDetailsBean td) {
        VehiclePosition.Builder position = VehiclePosition.newBuilder();
        if (record == null)
            return null;
        position.setTrip(makeTripDescriptor(td.getTrip(), td.getStatus()));
        position.setPosition(makePosition(record));
        position.setVehicle(makeVehicleDescriptor(record));
        position.setTimestamp(record.getTimeOfRecord()/1000);
        position.setStopId(td.getStatus().getNextStop().getId());

        return position;
    }

}
