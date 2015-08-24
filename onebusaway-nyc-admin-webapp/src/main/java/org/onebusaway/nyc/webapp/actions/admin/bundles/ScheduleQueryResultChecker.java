/**
 * 
 */
package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.BundleValidationCheckResult;

/**
 * 
 * This result checker will check the results from an api query searching for 
 * schedule information for a specific stop.
 * @author jpearson
 *
 */
public class ScheduleQueryResultChecker implements QueryResultChecker {

  @Override
  public BundleValidationCheckResult checkResults(BundleValidateQuery query) {
    BundleValidationCheckResult checkResult = new BundleValidationCheckResult();
    String result = query.getQueryResult();
    if (result.contains(CODE_200) && 
        (result.contains("arrivalEnabled") || result.contains("departureEnabled"))) {
      checkResult.setTestStatus(PASS);
      checkResult.setTestResult(query.getErrorMessage() + FOUND_SCHEDULE_ENTRIES + query.getStopId());
    } else {
      checkResult.setTestStatus(FAIL);
      checkResult.setTestResult(query.getErrorMessage() + DID_NOT_FIND_SCHEDULE_ENTRIES + query.getStopId());
    }

    return checkResult;
  }

}
