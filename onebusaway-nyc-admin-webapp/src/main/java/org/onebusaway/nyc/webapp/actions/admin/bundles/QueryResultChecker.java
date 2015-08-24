/**
 * 
 */
package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.util.List;

import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.BundleValidationCheckResult;

/**
 * 
 * This is the interface to a set of classes, each of which will evaluate the
 * result returned from a specific type of query used for validating a
 * transit data bundle.
 * @author jpearson
 *
 */
public interface QueryResultChecker {
  public static final String SHORT_NAME = "shortname\":\"";
  public static final String LONG_NAME = "longname\":\"";
  
  // Result messages
  public static final String FOUND_SCHEDULE_ENTRIES = "Found schedule entries for stop ";
  public static final String DID_NOT_FIND_SCHEDULE_ENTRIES = "Did not find schedule entries for stop ";
  public static final String FOUND_REALTIME_INFO = "Found real time info for stop ";
  public static final String DID_NOT_FIND_REALTIME_INFO = "Did not find real time info for stop ";
  public static final String FOUND_ROUTE_INFO = "Found information for route ";
  public static final String DID_NOT_FIND_ROUTE_INFO = "Did not find information for route ";
  
  public static final String PASS = "Pass";
  public static final String FAIL = "Fail";
  public static final String CODE_200 = "\"code\":200";

  public BundleValidationCheckResult checkResults(BundleValidateQuery query);
}
