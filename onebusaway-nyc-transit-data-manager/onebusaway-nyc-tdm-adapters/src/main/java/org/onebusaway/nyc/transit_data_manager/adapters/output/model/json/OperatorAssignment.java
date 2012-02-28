package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

/**
 * A model class representing a single operator - run assignment. For use in
 * creating JSON with Gson.
 * 
 * @author sclark
 * 
 */
public class OperatorAssignment {
  public OperatorAssignment() {

  }

  private String agencyId;
  private String passId;
  private String runRoute;
  private String runNumber;
  private String depot;
  private String serviceDate;
  private String updated;

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public void setPassId(String passId) {
    this.passId = passId;
  }

  public void setRunRoute(String runRoute) {
    this.runRoute = runRoute;
  }

  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }

  public void setDepot(String depot) {
    this.depot = depot;
  }

  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

}
