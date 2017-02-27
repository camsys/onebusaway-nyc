package org.onebusaway.nyc.gtfsrt.service;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.realtime.api.VehicleLocationRecord;

public interface VehicleUpdateFeedBuilder {

    GtfsRealtime.VehiclePosition getVehicleUpdateFromInferredLocation(VehicleLocationRecord record);
}
