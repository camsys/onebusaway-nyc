package org.onebusaway.nyc.admin.model;

/**
 * 
 * This class encapsulates any errors that occurred during the parsing of a bundle validation check file.
 * @author jpearson
 *
 */
public class BundleValidationParseError {
  public int linenum;
  public String errorMessage;
  public String offendingLine;
  public int getLinenum() {
    return linenum;
  }
  public void setLinenum(int linenum) {
    this.linenum = linenum;
  }
  public String getErrorMessage() {
    return errorMessage;
  }
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
  public String getOffendingLine() {
    return offendingLine;
  }
  public void setOffendingLine(String offendingLine) {
    this.offendingLine = offendingLine;
  }
}
