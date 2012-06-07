package org.onebusaway.nyc.admin.service;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;

public interface BundleRequestService {
  BundleResponse validate(BundleRequest bundleRequest);

  BundleResponse lookupValidationRequest(String id);
  BundleBuildResponse lookupBuildRequest(String id);
  BundleBuildResponse build(BundleBuildRequest bundleRequest, String responseId);
  
  /**
   * Builds and returns the URL where build bundle results can be viewed after the process completes
   * @param request bundle request
   * @return the result URL
   */
  BundleBuildResponse buildBundleResultURL(String bundleName);
  
  void cleanup();

  
}
