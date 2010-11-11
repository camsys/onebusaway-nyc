package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.gtfs.model.AgencyAndId;

public class VehicleLocationManagementRecord {

  private AgencyAndId vehicleId;

  private boolean enabled;

  private String mostRecentDestinationSignCode;

  private String inferredDestinationSignCode;

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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
