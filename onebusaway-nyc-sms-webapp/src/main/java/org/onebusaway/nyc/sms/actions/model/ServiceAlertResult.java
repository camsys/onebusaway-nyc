package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.model.SearchResult;

public class ServiceAlertResult implements SearchResult {

  private String alert;
  
  public ServiceAlertResult(String alert) {
    this.alert = alert;
  }
  
  public String getAlert() {
    return alert;
  }
  
}
