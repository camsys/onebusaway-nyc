package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * We're at a transit base. Are we ready to leave yet?
 * 
 * @author bdferris
 */
public class JourneyAtBaseMotionModel implements JourneyMotionModel {

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

    if (_stateTransitionService.isAtBase(edgeState))
      return _stateTransitionService.transitionToBase(
          parentJourneyState.getBlockState(), obs);

    // If we're not a base, we must be starting a block
    return _stateTransitionService.potentiallyStartBlock(edgeState,
        parentJourneyState.getBlockState(), obs, edgeState.getLocationOnEdge());
  }
}
