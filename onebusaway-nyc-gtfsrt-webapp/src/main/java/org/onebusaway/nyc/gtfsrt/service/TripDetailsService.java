package org.onebusaway.nyc.gtfsrt.service;

import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;

public interface TripDetailsService {
    TripDetailsBean getTripDetailsForVehicleLocationRecord(VehicleLocationRecordBean bean);
}
