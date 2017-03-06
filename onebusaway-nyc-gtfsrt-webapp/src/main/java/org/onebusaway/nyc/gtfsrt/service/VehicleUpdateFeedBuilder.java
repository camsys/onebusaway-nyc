package org.onebusaway.nyc.gtfsrt.service;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;

public interface VehicleUpdateFeedBuilder {

    GtfsRealtime.VehiclePosition makeVehicleUpdate(VehicleLocationRecordBean status, TripDetailsBean td);
}
