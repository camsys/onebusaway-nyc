package org.onebusaway.nyc.admin.model;

import java.util.List;

/**
 * This is the result returned from parsing a file of bundle validation checks.
 * @author jpearson
 *
 */
public class BundleValidationParseResults {
  public List<ParsedBundleValidationCheck> parsedBundleChecks;
  public List<BundleValidationParseError> parseErrors;
  
  public List<ParsedBundleValidationCheck> getParsedBundleChecks() {
    return parsedBundleChecks;
  }
  public void setParsedBundleChecks(
      List<ParsedBundleValidationCheck> parsedBundleChecks) {
    this.parsedBundleChecks = parsedBundleChecks;
  }
  public List<BundleValidationParseError> getParseErrors() {
    return parseErrors;
  }
  public void setParseErrors(List<BundleValidationParseError> parseErrors) {
    this.parseErrors = parseErrors;
  }
}
