package org.onebusaway.nyc.presentation.impl;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.model.realtime.VehicleResult;
import org.onebusaway.nyc.presentation.service.RealtimeModelFactory;

public class DefaultRealtimeModelFactory implements RealtimeModelFactory {

  public DistanceAway getDistanceAwayModel() {
    return new DistanceAway();
  }

  public VehicleResult getVehicleLocationModel() {
    return new VehicleResult();
  }

}