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
  
  void cleanup();

  
}
