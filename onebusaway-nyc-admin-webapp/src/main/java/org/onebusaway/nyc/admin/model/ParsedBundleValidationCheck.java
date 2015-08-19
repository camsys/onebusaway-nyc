package org.onebusaway.nyc.admin.model;

/**
 * Results of parsing the Bundle Validation Checks csv file.
 * @author jpearson
 *
 */
public class ParsedBundleValidationCheck {
  private int linenum;
  private String route;
  private String specificTest;
  private String routeOrStop;
  private String URI;
  
  public int getLinenum() {
    return linenum;
  }
  public void setLinenum(int linenum) {
    this.linenum = linenum;
  }
  public String getRoute() {
    return route;
  }
  public void setRoute(String route) {
    this.route = route;
  }
  public String getSpecificTest() {
    return specificTest;
  }
  public void setSpecificTest(String specificTest) {
    this.specificTest = specificTest;
  }
  public String getRouteOrStop() {
    return routeOrStop;
  }
  public void setRouteOrStop(String routeOrStop) {
    this.routeOrStop = routeOrStop;
  }
  public String getURI() {
    return URI;
  }
  public void setURI(String uRI) {
    URI = uRI;
  }
}
