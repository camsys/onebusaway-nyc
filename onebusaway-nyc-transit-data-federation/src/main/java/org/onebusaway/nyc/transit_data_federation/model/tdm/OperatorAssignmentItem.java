package org.onebusaway.nyc.transit_data_federation.model.tdm;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;

import org.joda.time.DateTime;

import java.io.Serializable;

public class OperatorAssignmentItem implements Serializable {

	private static final long serialVersionUID = -2435563339750796076L;

	private String agencyId;

	private String passId;

	private String runRoute;

	private ServiceDate serviceDate;

	private DateTime updated;

	private String runNumber;

	private String depot;
	
	private String runId;

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

	public ServiceDate getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(ServiceDate serviceDate) {
		this.serviceDate = serviceDate;
	}

	public DateTime getUpdated() {
		return updated;
	}

	public void setUpdated(DateTime updated) {
		this.updated = updated;
	}

	public void setDepot(String depot) {
		this.depot = depot;
	}

	public String getDepot() {
		return depot;
	}

	public String getRunId() {
	  return runId;
	}

  public void setRunId(String runId) {
    this.runId = runId;
  }

}
