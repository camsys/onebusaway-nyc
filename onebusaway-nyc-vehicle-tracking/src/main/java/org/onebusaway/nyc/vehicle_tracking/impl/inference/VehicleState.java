package org.onebusaway.nyc.vehicle_tracking.impl.inference;

/**
 * We make this class immutable so that we don't have to worry about particle
 * filter methods changing it out from under us
 * 
 * @author bdferris
 * 
 */
public class VehicleState {

  /**
   * Our location on an edge in the street graph
   */
  private final EdgeState edgeState;

  /**
   * Our current block state
   */
  private final BlockState blockState;

  public VehicleState(VehicleState state) {
    this.edgeState = state.edgeState;
    this.blockState = state.blockState;
  }

  public VehicleState(EdgeState edgeState, BlockState blockState) {
    if( edgeState == null)
      throw new IllegalArgumentException("edgeState cannot be null");
    if( blockState == null)
      throw new IllegalArgumentException("blockState cannot be null");
    this.edgeState = edgeState;
    this.blockState = blockState;
  }

  public EdgeState getEdgeState() {
    return edgeState;
  }

  public BlockState getBlockState() {
    return blockState;
  }
}
