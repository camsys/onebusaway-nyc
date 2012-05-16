package org.onebusaway.nyc.admin.service;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;

public interface BundleRequestService {
  BundleResponse validate(BundleRequest bundleRequest);
}
