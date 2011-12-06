package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;

public class BlockStateTransDistParams {
  Observation _obs;
  BlockState _blockState;
  
  public BlockStateTransDistParams(Observation obs, BlockState blockState) {
    _obs = obs;
    _blockState = blockState;
  }

  BlockState getBlockState() {
    return _blockState;
  }
  
  Observation getObservation() {
    return _obs;
  }
}
