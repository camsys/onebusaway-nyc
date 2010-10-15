package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * We're dead heading before the start of a block
 * 
 * @author bdferris
 */
public class JourneyDeadheadBeforeMotionModel implements JourneyMotionModel {

  private StateTransitionService _stateTransitionService;

  @Autowired
  public void setStateTransitionService(
      StateTransitionService stateTransitionService) {
    _stateTransitionService = stateTransitionService;
  }

  /****
   * {@link JourneyMotionModel} Interface
   ****/

  @Override
  public JourneyState move(Particle parent, VehicleState parentState,
      EdgeState edgeState, MotionState motionState, Observation obs,
      JourneyState parentJourneyState) {

    JourneyStartState start = parentJourneyState.getData();

    return _stateTransitionService.potentiallyStartBlock(edgeState,
        parentJourneyState.getBlockState(), obs, start.getJourneyStart());
  }

}
