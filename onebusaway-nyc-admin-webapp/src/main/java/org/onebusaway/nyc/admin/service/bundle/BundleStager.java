package org.onebusaway.nyc.admin.service.bundle;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleStatus;


public interface BundleStager {
  public String getBuiltBundleDirectory();

  public String getStagedBundleDirectory();
  
  List<String> listBundlesForServing(String path);
  
  void stage(BundleStatus status, String environment, String bundleDir, String bundleName);
  
  void notifyOTP(String bundleName) throws Exception;
}
