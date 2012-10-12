package org.onebusaway.nyc.transit_data_manager.config;

import org.onebusaway.nyc.transit_data_manager.bundle.model.ConfigDeployStatus;

import java.util.List;

public interface ConfigurationDeployer {

  void deploy(ConfigDeployStatus status, String s3Path);

  void setUser(String user);

  void setPassword(String password);

  void setBucketName(String bucketName);

  void setup();

  void setLocalDepotIdMapDir(String dir);

  void setLocalDscFileDir(String dir);

  List<String> listFiles(String s3Path);
}
