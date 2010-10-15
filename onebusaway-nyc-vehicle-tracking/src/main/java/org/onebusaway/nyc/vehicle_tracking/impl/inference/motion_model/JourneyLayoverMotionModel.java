package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model.JourneyLayoverSensorModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class JourneyLayoverMotionModel implements JourneyMotionModel {

  private JourneyLayoverSensorModelImpl _sensorModel;
  
  private StateTransitionService _stateTransitionService;

  @Autowired
  public void setSensorModel(JourneyLayoverSensorModelImpl sensorModel) {
    _sensorModel = sensorModel;
  }
  
  @Autowired
  public void setStateTransitionService(
      StateTransitionService stateTransitionService) {
    _stateTransitionService = stateTransitionService;
  }

  /****
   * {@link MotionModel} Interface
   ****/

  @Override
  public JourneyState move(Particle parent, VehicleState parentState,
      EdgeState edgeState, MotionState motionState, Observation obs,
      JourneyState parentJourneyState) {

    // Should we stay in our layover?
    double pLayover = _sensorModel.likelihood(obs, edgeState, motionState,
        parentJourneyState.getBlockState());

    if (Math.random() < pLayover)
      return parentJourneyState;

    // Exit the layover
    return JourneyState.inProgress(parentJourneyState.getBlockState());
  }
}
