package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.gtfs.model.AgencyAndId;

public interface RunService {
  String getInitialRunForTrip(AgencyAndId trip);

  String getReliefRunForTrip(AgencyAndId trip);

  int getReliefTimeForTrip(AgencyAndId trip);
}
