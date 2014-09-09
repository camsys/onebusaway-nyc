package org.onebusaway.nyc.admin.service.bundle;


public interface BundleStager {
  public String getBuiltBundleDirectory();

  public String getStagedBundleDirectory();
  
  void stage(String env, String bundleDir, String bundleName) throws Exception;
  
  void notifyOTP(String bundleName) throws Exception;
}
