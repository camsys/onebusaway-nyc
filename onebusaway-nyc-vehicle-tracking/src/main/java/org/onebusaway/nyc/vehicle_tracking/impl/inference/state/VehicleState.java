package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

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

  private final JourneyState journeyState;

  public VehicleState(VehicleState state) {
    this.edgeState = state.edgeState;
    this.motionState = state.motionState;
    this.journeyState = state.journeyState;
  }

  public VehicleState(EdgeState edgeState, MotionState motionState,
      JourneyState journeyState) {
    if (edgeState == null)
      throw new IllegalArgumentException("edgeState cannot be null");
    if (motionState == null)
      throw new IllegalArgumentException("motionState cannot be null");
    if (journeyState == null)
      throw new IllegalArgumentException("journeyPhase cannot be null");
    this.edgeState = edgeState;
    this.motionState = motionState;
    this.journeyState = journeyState;
  }

  public EdgeState getEdgeState() {
    return edgeState;
  }

  public MotionState getMotionState() {
    return motionState;
  }

  public JourneyState getJourneyState() {
    return journeyState;
  }
}
