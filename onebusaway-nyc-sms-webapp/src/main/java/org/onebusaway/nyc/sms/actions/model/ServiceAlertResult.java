package org.onebusaway.nyc.sms.actions.model;

import java.io.Serializable;

import org.onebusaway.nyc.presentation.model.SearchResult;

public class ServiceAlertResult implements SearchResult, Serializable {

	private static final long serialVersionUID = 1L;

	private String alert;
	
	private Double distanceToQueryLocation = null;

	public ServiceAlertResult(String alert) {
		this.alert = alert;
	}

	public String getAlert() {
		return alert;
	}

	@Override
  public void setDistanceToQueryLocation(Double distance) {
    this.distanceToQueryLocation = distance;
  }

  @Override
  public Double getDistanceToQueryLocation() {
    return this.distanceToQueryLocation;
  }

}
