package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.BundleValidationCheckResult;

/**
 * 
 * This result checker will check the results from a siri query searching for 
 * realtime information for a specific stop.
 * @author jpearson
 *
 */
public class RealtimeQueryResultChecker implements QueryResultChecker {

  @Override
  public BundleValidationCheckResult checkResults(BundleValidateQuery query) {
    BundleValidationCheckResult checkResult = new BundleValidationCheckResult();
    String result = query.getQueryResult();
    if (result.contains("ExpectedArrival") || result.contains("ExpectedDeparture")) {
      checkResult.setTestStatus(PASS);
      checkResult.setTestResult(query.getErrorMessage() + FOUND_REALTIME_INFO + query.getStopId());
    } else {
      checkResult.setTestStatus(FAIL);
      checkResult.setTestResult(query.getErrorMessage() + DID_NOT_FIND_REALTIME_INFO + query.getStopId());
    }
    return checkResult;
  }

}
