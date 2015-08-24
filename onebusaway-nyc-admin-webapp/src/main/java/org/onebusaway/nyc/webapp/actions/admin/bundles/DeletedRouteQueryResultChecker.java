/**
 * 
 */
package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.BundleValidationCheckResult;

/**
 * 
 * This result checker will check the results from an api query searching for 
 * route information for a specific route and verify that the route is no
 * longer listed under the specified agency.
 * @author jpearson
 *
 */
public class DeletedRouteQueryResultChecker implements QueryResultChecker {

  @Override
  public BundleValidationCheckResult checkResults(BundleValidateQuery query) {
    BundleValidationCheckResult checkResult = new BundleValidationCheckResult();
    String result = query.getQueryResult();
    result = result.toLowerCase();
    String route = query.getRouteOrStop();
    route = route.toLowerCase();
    String routeId = query.getRouteId();
    if (routeId.length() == 0) {
      routeId = query.getRouteOrStop();
    }

    if (result.contains(CODE_200) && 
        (!result.contains(SHORT_NAME + route) && !result.contains(LONG_NAME + route))) {
      checkResult.setTestStatus(PASS);
      checkResult.setTestResult(query.getErrorMessage() + FOUND_ROUTE_INFO + routeId);
    } else {
      checkResult.setTestStatus(FAIL);
      checkResult.setTestResult(query.getErrorMessage() + DID_NOT_FIND_ROUTE_INFO + routeId);
    }       

    return checkResult;
  }

}
