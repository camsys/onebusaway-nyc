package org.onebusaway.nyc.admin.service.bundle;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;

public interface BundleDeployer {
  void setup();
  List<String> listFiles(String directory, int maxResults);
  void deploy(BundleDeployStatus status, String path);
  List<String> listBundlesForServing(String path);
}
