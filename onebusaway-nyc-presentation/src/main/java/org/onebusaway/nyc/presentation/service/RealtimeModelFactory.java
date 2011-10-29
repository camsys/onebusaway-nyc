package org.onebusaway.nyc.presentation.service;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.model.realtime.VehicleResult;

public abstract interface RealtimeModelFactory {

  public DistanceAway getDistanceAwayModel();
 
  public VehicleResult getVehicleLocationModel();
 
}