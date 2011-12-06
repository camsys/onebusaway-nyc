package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;

public class ScheduleDevParams {
  
  private final BlockState _state;
  private final Observation _obs;
  
  public ScheduleDevParams(Observation obs, BlockState state) {
    _state = state;
    _obs = obs;
  }

  public BlockState getBlockState() {
    return _state;
  }

  public Observation getObservation() {
    return _obs;
  }
  

}
