package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;

public final class NycVehicleStatusBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String vehicleId;

  private boolean enabled;

  private long lastUpdateTime;

  private long lastGpsTime;

  private double lastGpsLat = Double.NaN;

  private double lastGpsLon = Double.NaN;

  private String phase;

  private String status;

  private String mostRecentDestinationSignCode;

  private String inferredDestinationSignCode;

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public long getLastGpsTime() {
    return lastGpsTime;
  }

  public void setLastGpsTime(long lastGpsTime) {
    this.lastGpsTime = lastGpsTime;
  }

  public double getLastGpsLat() {
    return lastGpsLat;
  }

  public void setLastGpsLat(double lastGpsLat) {
    this.lastGpsLat = lastGpsLat;
  }

  public double getLastGpsLon() {
    return lastGpsLon;
  }

  public void setLastGpsLon(double lastGpsLon) {
    this.lastGpsLon = lastGpsLon;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMostRecentDestinationSignCode() {
    return mostRecentDestinationSignCode;
  }

  public void setMostRecentDestinationSignCode(
      String mostRecentDestinationSignCode) {
    this.mostRecentDestinationSignCode = mostRecentDestinationSignCode;
  }

  public String getInferredDestinationSignCode() {
    return inferredDestinationSignCode;
  }

  public void setInferredDestinationSignCode(String inferredDestinationSignCode) {
    this.inferredDestinationSignCode = inferredDestinationSignCode;
  }
}
