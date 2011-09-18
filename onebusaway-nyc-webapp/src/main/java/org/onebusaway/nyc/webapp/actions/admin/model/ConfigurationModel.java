package org.onebusaway.nyc.webapp.actions.admin.model;

public class ConfigurationModel {

  private Integer noProgressTimeout;

  private Integer offRouteDistance;

  private Integer staleDataTimeout;

  private Integer hideTimeout;

  private Integer gpsTimeSkewThreshold;
  
  public ConfigurationModel() {}

  public Integer getNoProgressTimeout() {
    return noProgressTimeout;
  }

  public void setNoProgressTimeout(Integer noProgressTimeout) {
    this.noProgressTimeout = noProgressTimeout;
  }

  public Integer getOffRouteDistance() {
    return offRouteDistance;
  }

  public void setOffRouteDistance(Integer offRouteDistance) {
    this.offRouteDistance = offRouteDistance;
  }

  public Integer getStaleDataTimeout() {
    return staleDataTimeout;
  }

  public void setStaleDataTimeout(Integer staleDataTimeout) {
    this.staleDataTimeout = staleDataTimeout;
  }

  public Integer getHideTimeout() {
    return hideTimeout;
  }

  public void setHideTimeout(Integer hideTimeout) {
    this.hideTimeout = hideTimeout;
  }

  public Integer getGpsTimeSkewThreshold() {
	  return gpsTimeSkewThreshold;
  }

  public void setGpsTimeSkewThreshold(Integer t) {
	  this.gpsTimeSkewThreshold = t;
  }

}