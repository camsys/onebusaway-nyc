package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * We make this class immutable so that we don't have to worry about particle
 * filter methods changing it out from under us
 * 
 * @author bdferris
 * 
 */
public class VehicleState {

  private final AgencyAndId tripId;

  private final double positionDeviation;

  private final EdgeState edgeState;

  public VehicleState(VehicleState state) {
    this.tripId = state.tripId;
    this.positionDeviation = state.positionDeviation;
    this.edgeState = state.edgeState;
  }

  private VehicleState(Builder builder) {
    this.tripId = builder.tripId;
    this.positionDeviation = builder.positionDeviation;
    this.edgeState = builder.edgeState;
  }

  public static Builder builder() {
    return new Builder();
  }

  public AgencyAndId getTripId() {
    return tripId;
  }

  public double getPositionDeviation() {
    return positionDeviation;
  }

  public EdgeState getEdgeState() {
    return edgeState;
  }

  public static class Builder {

    private AgencyAndId tripId = null;

    private double positionDeviation;

    private EdgeState edgeState = null;

    public VehicleState create() {
      return new VehicleState(this);
    }

    public void setTripId(AgencyAndId tripId) {
      this.tripId = tripId;
    }

    public void setPositionDeviation(double positionDeviation) {
      this.positionDeviation = positionDeviation;
    }

    public void setEdgeState(EdgeState edgeState) {
      this.edgeState = edgeState;
    }
  }
}
