package org.onebusaway.nyc.transit_data_manager.adapters.input.model;

public class MtaBusDepotAssignment {

  public MtaBusDepotAssignment() {

  }

  private String depot;
  private Long agencyId;
  private int busNumber;

  public String getDepot() {
    return depot;
  }

  public Long getAgencyId() {
    return agencyId;
  }

  public int getBusNumber() {
    return busNumber;
  }

  public void setDepot(String depot) {
    this.depot = depot;
  }

  public void setAgencyId(Long agencyId) {
    this.agencyId = agencyId;
  }

  public void setBusNumber(int busNumber) {
    this.busNumber = busNumber;
  }

}
