package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.springframework.beans.factory.annotation.Autowired;

public class JourneyUnknownMotionModel implements JourneyMotionModel {

  private StateTransitionService _stateTransitionService;

  @Autowired
  public void setStateTransitionService(
      StateTransitionService stateTransitionService) {
    _stateTransitionService = stateTransitionService;
  }

  @Override
  public JourneyState move(Particle parent, VehicleState parentState,
      EdgeState edgeState, MotionState motionState, Observation obs,
      JourneyState parentJourneyState) {

    // Did we arrive at a base?
    if (_stateTransitionService.isAtBase(edgeState))
      return _stateTransitionService.transitionToBase(
          parentJourneyState.getBlockState(), obs);

    // If we shouldn't go back in service, we just carry on
    if (!_stateTransitionService.shouldWeGoBackInService(obs))
      return parentJourneyState;

    return _stateTransitionService.potentiallyStartOrContinueBlock(edgeState,
        obs);
  }
}
