package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;

public final class NycVehicleStatusBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String vehicleId;

  private String mostRecentDestinationSignCode;

  private String inferredDestinationSignCode;

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
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
