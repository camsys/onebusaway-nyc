package org.onebusaway.nyc.admin.service.bundle;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleStatus;

public interface BundleDeployer {
  void setup();
  void deploy(BundleStatus status, String path);
  List<String> listStagedBundles(String path);
}
