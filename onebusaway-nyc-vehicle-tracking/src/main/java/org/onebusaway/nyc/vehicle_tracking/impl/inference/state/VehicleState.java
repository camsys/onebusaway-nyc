package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

/**
 * We make this class immutable so that we don't have to worry about particle
 * filter methods changing it out from under us
 * 
 * @author bdferris
 * 
 */
public final class VehicleState {

  /**
   * Our location on an edge in the street graph
   */
  private final EdgeState edgeState;

  private final MotionState motionState;

  private final BlockState blockState;

  private final JourneyState journeyState;

  private final Observation observation;

  public VehicleState(VehicleState state) {
    this.edgeState = state.edgeState;
    this.motionState = state.motionState;
    this.blockState = state.blockState;
    this.journeyState = state.journeyState;
    this.observation = state.observation;
  }

  public VehicleState(EdgeState edgeState, MotionState motionState,
      BlockState blockState, JourneyState journeyState, Observation observation) {
    if (motionState == null)
      throw new IllegalArgumentException("motionState cannot be null");
    if (journeyState == null)
      throw new IllegalArgumentException("journeyPhase cannot be null");
    if (observation == null)
      throw new IllegalArgumentException("observation cannot be null");
    this.edgeState = edgeState;
    this.motionState = motionState;
    this.blockState = blockState;
    this.journeyState = journeyState;
    this.observation = observation;
  }

  public EdgeState getEdgeState() {
    return edgeState;
  }

  public MotionState getMotionState() {
    return motionState;
  }

  public BlockState getBlockState() {
    return blockState;
  }

  public JourneyState getJourneyState() {
    return journeyState;
  }

  public Observation getObservation() {
    return observation;
  }

  @Override
  public String toString() {
    return journeyState + " " + blockState + " " + observation;
  }
}
