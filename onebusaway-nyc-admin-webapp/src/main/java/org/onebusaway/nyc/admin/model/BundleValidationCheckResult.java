package org.onebusaway.nyc.admin.model;

/**
 * This is the result created by running a BundleValidationCheck query and 
 * assessing the returned value to determine success or failure.
 * @author jpearson
 *
 */
public class BundleValidationCheckResult {
  private int linenum;
  private int csvLinenum;
  private String specificTest;
  private String testStatus;
  private String testResult;
  private String testQuery;
  
  public int getLinenum() {
    return linenum;
  }
  public void setLinenum(int linenum) {
    this.linenum = linenum;
  }
  public int getCsvLinenum() {
    return csvLinenum;
  }
  public void setCsvLinenum(int csvLinenum) {
    this.csvLinenum = csvLinenum;
  }
  public String getSpecificTest() {
    return specificTest;
  }
  public void setSpecificTest(String specificTest) {
    this.specificTest = specificTest;
  }
  public String getTestStatus() {
    return testStatus;
  }
  public void setTestStatus(String testStatus) {
    this.testStatus = testStatus;
  }
  public String getTestResult() {
    return testResult;
  }
  public void setTestResult(String testResult) {
    this.testResult = testResult;
  }
  public String getTestQuery() {
    return testQuery;
  }
  public void setTestQuery(String testQuery) {
    this.testQuery = testQuery;
  }
}
