package org.onebusaway.nyc.admin.model;

/**
 * Results of parsing the Bundle Validation Checks csv file.
 * @author jpearson
 *
 */
public class ParsedBundleValidationCheck {
  private int linenum=0;
  private String agencyId="";
  private String specificTest="";
  private String routeName="";
  private String routeId="";
  private String stopName="";
  private String stopId="";
  private String date="";
  private String departureTime="";
  
  public int getLinenum() {
    return linenum;
  }
  public void setLinenum(int linenum) {
    this.linenum = linenum;
  }
  public String getAgencyId() {
    return agencyId;
  }
  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }
  public String getSpecificTest() {
    return specificTest;
  }
  public void setSpecificTest(String specificTest) {
    this.specificTest = specificTest;
  }
  public String getRouteName() {
    return routeName;
  }
  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }
  public String getRouteId() {
    return routeId;
  }
  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }
  public String getStopName() {
    return stopName;
  }
  public void setStopName(String stopName) {
    this.stopName = stopName;
  }
  public String getStopId() {
    return stopId;
  }
  public void setStopId(String stopId) {
    this.stopId = stopId;
  }
  public String getDate() {
    return date;
  }
  public void setDate(String date) {
    this.date = date;
  }
  public String getDepartureTime() {
    return departureTime;
  }
  public void setDepartureTime(String departureTime) {
    this.departureTime = departureTime;
  }
}
