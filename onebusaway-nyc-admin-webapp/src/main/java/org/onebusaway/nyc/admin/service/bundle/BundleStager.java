package org.onebusaway.nyc.admin.service.bundle;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;

public interface BundleStager {
  void setup();
  void stage(String env, String bundleDir, String bundleName) throws Exception;
}
