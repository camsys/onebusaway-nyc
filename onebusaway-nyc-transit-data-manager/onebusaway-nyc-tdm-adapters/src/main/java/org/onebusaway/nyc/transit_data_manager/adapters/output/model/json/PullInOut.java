package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

public class PullInOut {
  private boolean isPullIn;
  private Vehicle vehicle;
  private String agencyId;
  private String passId;
  private String blockDesignator;
  private String runRoute;
  private String runNumber;
  private String depotName;
  private String serviceDate;
  private String time;

  public void setPullIn(boolean isPullIn) {
    this.isPullIn = isPullIn;
  }

  public void setVehicle(Vehicle vehicle) {
    this.vehicle = vehicle;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public void setPassId(String passId) {
    this.passId = passId;
  }

  public void setBlockDesignator(String blockDesignator) {
    this.blockDesignator = blockDesignator;
  }

  public void setRunRoute(String runRoute) {
    this.runRoute = runRoute;
  }

  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }

  public void setDepotName(String depotName) {
    this.depotName = depotName;
  }

  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
  }

  public void setTime(String time) {
    this.time = time;
  }
}
