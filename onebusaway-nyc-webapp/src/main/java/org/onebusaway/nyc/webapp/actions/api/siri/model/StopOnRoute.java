package org.onebusaway.nyc.webapp.actions.api.siri.model;

import org.onebusaway.transit_data.model.StopBean;

public class StopOnRoute {

  private StopBean stop;
  
  private Boolean hasUpcomingScheduledStop;
  
  public StopOnRoute(StopBean stop) {
    this.stop = stop;
  }
  
  public StopOnRoute(StopBean stop, Boolean hasUpcomingScheduledStop) {
	  this.stop = stop;
	  this.hasUpcomingScheduledStop = hasUpcomingScheduledStop;
  }
  
  public String getId() {
    return stop.getId();
  }
  
  public String getName() {
    return stop.getName();
  }
  
  public Double getLatitude() {
    return stop.getLat();
  }
  
  public Double getLongitude() {
    return stop.getLon();
  }

  public String getStopDirection() {
    if(stop.getDirection() == null || (stop.getDirection() != null && stop.getDirection().equals("?"))) {
      return "unknown";
    } else {
      return stop.getDirection();
    }
  }

  public Boolean getHasUpcomingScheduledStop() {
	  return hasUpcomingScheduledStop;
  }
	
  public void setHasUpcomingScheduledStop(Boolean hasUpcomingScheduledStop) {
	  this.hasUpcomingScheduledStop = hasUpcomingScheduledStop;
  }

}
