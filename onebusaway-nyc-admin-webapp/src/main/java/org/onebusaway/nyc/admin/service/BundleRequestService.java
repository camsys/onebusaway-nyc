package org.onebusaway.nyc.admin.service;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;

public interface BundleRequestService {
  BundleResponse validate(BundleRequest bundleRequest);

  BundleResponse lookupValidationRequest(String id);
  BundleBuildResponse lookupBuildRequest(String id);
  BundleBuildResponse build(BundleBuildRequest bundleRequest);
  
  /**
   * Builds and returns the URL where build bundle results can be viewed after the process completes
   * @param id the id of the bundle request
   * @return the response with the url set
   */
  BundleBuildResponse buildBundleResultURL(String Id);
  
  void cleanup();

  
}
