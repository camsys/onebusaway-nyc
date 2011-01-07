package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;

public class Context {

  private final VehicleState parentState;

  private final VehicleState state;

  private final Observation observation;

  public Context(VehicleState parentState, VehicleState state,
      Observation observation) {
    this.parentState = parentState;
    this.state = state;
    this.observation = observation;
  }

  public VehicleState getParentState() {
    return parentState;
  }

  public VehicleState getState() {
    return state;
  }

  public Observation getObservation() {
    return observation;
  }
}
