package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.gtfs.model.AgencyAndId;

public class VehicleState {

  private double lat;

  private double lon;

  private AgencyAndId tripId;

  private double positionDeviation;

  public VehicleState() {
    
  }
  
  public VehicleState(VehicleState state) {
    this.lat = state.lat;
    this.lon = state.lon;
    this.tripId = state.tripId;
    this.positionDeviation = state.positionDeviation;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public AgencyAndId getTripId() {
    return tripId;
  }

  public void setTripId(AgencyAndId tripId) {
    this.tripId = tripId;
  }

  public double getPositionDeviation() {
    return positionDeviation;
  }

  public void setPositionDeviation(double positionDeviation) {
    this.positionDeviation = positionDeviation;
  }
}
