package org.onebusaway.nyc.admin.model;

/**
 * API queries generated from the parsed csv bundle check file to be used to test 
 * the Bundle Validation Checks.
 * @author jpearson
 *
 */
public class BundleValidateQuery {
  private int linenum;
  private String specificTest;
  private String routeOrStop;
  private String query;
  private String queryResult;
  
  public int getLinenum() {
    return linenum;
  }
  public void setLinenum(int linenum) {
    this.linenum = linenum;
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
  public String getQuery() {
    return query;
  }
  public void setQuery(String query) {
    this.query = query;
  }
  public String getQueryResult() {
    return queryResult;
  }
  public void setQueryResult(String queryResult) {
    this.queryResult = queryResult;
  }
}
