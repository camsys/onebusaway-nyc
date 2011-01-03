package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.geospatial.model.CoordinatePoint;

public interface BaseLocationService {

  public String getBaseNameForLocation(CoordinatePoint location);

  public String getTerminalNameForLocation(CoordinatePoint location);
}
