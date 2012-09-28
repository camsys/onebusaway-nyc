package org.onebusaway.nyc.transit_data_manager.bundle;


import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;

import java.util.List;

public interface BundleDeployer {
  void setup();
  String get(String s3Key, String destinationDirectory);
  void setUser(String user);
  void setPassword(String password);
  void setBucketName(String bucketName);
  List<String> listFiles(String directory, int maxResults);
  void deploy(BundleDeployStatus status, String s3Path);
  List<String> listBundlesForServing(String s3Path);
}
