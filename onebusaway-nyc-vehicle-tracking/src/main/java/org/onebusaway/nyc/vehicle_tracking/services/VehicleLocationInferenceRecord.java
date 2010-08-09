package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Information we need for performing vehicle location inference.
 * 
 * @author bdferris
 */
public class VehicleLocationInferenceRecord {

  private long timestamp;

  private AgencyAndId vehicleId;

  private String destinationSignCode;

  private double lat;

  private double lon;

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  public void setDestinationSignCode(String destinationSignCode) {
    this.destinationSignCode = destinationSignCode;
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
}
