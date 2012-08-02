package org.onebusaway.nyc.transit_data_manager.json.model;

public class RouteDetail {
  private String dsc;
  private String agencyId;
  private String routeId;
  private String description;
  private String shortName;
  private String longName;

  public String getDsc() {
    return dsc;
  }

  public void setDsc(String dsc) {
    this.dsc = dsc;
  }

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public String getRouteId() {
    return routeId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public String getLongName() {
    return longName;
  }

  public void setLongName(String longName) {
    this.longName = longName;
  }
}
