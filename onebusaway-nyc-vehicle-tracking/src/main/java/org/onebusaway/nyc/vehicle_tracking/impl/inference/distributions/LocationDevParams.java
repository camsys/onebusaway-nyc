package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;

public class LocationDevParams {

  private final Observation _obs;
  private final BlockState _blockState;

  public LocationDevParams(Observation obs, BlockState blockState) {
    _obs = obs;
    _blockState = blockState;
  }

  public Observation getObservation() {
    return _obs;
  }

  public BlockState getBlockState() {
    return _blockState;
  }

}
