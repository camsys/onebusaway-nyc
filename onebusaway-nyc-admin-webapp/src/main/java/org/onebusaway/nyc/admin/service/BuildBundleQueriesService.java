/**
 * 
 */
package org.onebusaway.nyc.admin.service;

import java.util.List;

import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.ParsedBundleValidationCheck;

/**
 * 
 * For building the api queries that will be used to check the validity of a transit data bundle.
 * The queries are generated from parsed data returned from BundleCheckParserService.
 * @author jpearson
 *
 */
public interface BuildBundleQueriesService {
  public List<BundleValidateQuery> buildQueries(List<ParsedBundleValidationCheck> parsedChecks, 
      String checkEnvironment);
}
