package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.transit_data_federation.services.tripplanner.TripInstanceProxy;

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
   * Our currently assigned trip instance
   */
  private final TripInstanceProxy tripInstance;

  /**
   * Distance, in meters, the vehicle has traveled along its current trip
   */
  private final double tripPositionOffset;

  /**
   * What we think the destination sign code SHOULD be
   */
  private String destinationSignCode;

  public VehicleState(VehicleState state) {
    this.edgeState = state.edgeState;
    this.tripInstance = state.tripInstance;
    this.tripPositionOffset = state.tripPositionOffset;
    this.destinationSignCode = state.destinationSignCode;
  }

  private VehicleState(Builder builder) {
    this.edgeState = builder.edgeState;
    this.tripInstance = builder.tripInstance;
    this.tripPositionOffset = builder.tripPositionOffset;
    this.destinationSignCode = builder.destinationSignCode;
  }

  public static Builder builder() {
    return new Builder();
  }

  public EdgeState getEdgeState() {
    return edgeState;
  }

  public TripInstanceProxy getTripInstance() {
    return tripInstance;
  }

  public double getTripPositionOffset() {
    return tripPositionOffset;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  public static class Builder {

    private EdgeState edgeState = null;

    private TripInstanceProxy tripInstance = null;

    private double tripPositionOffset;

    private String destinationSignCode = null;

    public VehicleState create() {
      return new VehicleState(this);
    }

    public void setEdgeState(EdgeState edgeState) {
      this.edgeState = edgeState;
    }

    public void setTripInstance(TripInstanceProxy tripInstance) {
      this.tripInstance = tripInstance;
    }

    public void setTripPositionOffset(double tripPositionOffset) {
      this.tripPositionOffset = tripPositionOffset;
    }

    public void setDestinationSignCode(String destinationSignCode) {
      this.destinationSignCode = destinationSignCode;
    }
  }
}
