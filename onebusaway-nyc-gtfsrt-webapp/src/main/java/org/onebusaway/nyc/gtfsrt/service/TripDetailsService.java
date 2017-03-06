package org.onebusaway.nyc.gtfsrt.service;

import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;

public interface TripDetailsService {
    TripDetailsBean getTripDetailsForVehicleStatus(VehicleStatusBean bean);
}
