package org.onebusaway.nyc.transit_data_federation.model.tdm;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

public class OperatorAssignmentItem implements Serializable {
	
	private static final long serialVersionUID = -2435563339750796076L;

	private String agencyId;

	private String passId;

	private String runRoute;

	private Date serviceDate;

	private DateTime updated;

  private String runNumber;
	
	public OperatorAssignmentItem() {}

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public String getPassId() {
    return passId;
  }

  public void setPassId(String passId) {
    this.passId = passId;
  }

  public String getRunRoute() {
    return runRoute;
  }

  public void setRunRoute(String runRoute) {
    this.runRoute = runRoute;
  }

  public String getRunNumber() {
    return runNumber;
  }
  
  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }

  public String getRunId() {
    return runRoute + "_" + runNumber;
  }

  public Date getServiceDate() {
    return serviceDate;
  }

  public void setServiceDate(Date serviceDate) {
    this.serviceDate = serviceDate;
  }

  public DateTime getUpdated() {
    return updated;
  }

  public void setUpdated(DateTime updated) {
    this.updated = updated;
  }

}
