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
  private String runId;

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

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

}
