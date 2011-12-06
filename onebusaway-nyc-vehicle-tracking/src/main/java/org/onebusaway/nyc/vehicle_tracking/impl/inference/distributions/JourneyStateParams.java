package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;

public class JourneyStateParams {

  private final Observation _obs;
  private final BlockState _blockState;

  public JourneyStateParams(Observation obs, BlockState blockState) {
    _obs = obs;
    _blockState = blockState;
  }
  public BlockState getBlockState() {
    return _blockState;
  }

  public Observation getObservation() {
    return _obs;
  }

}
