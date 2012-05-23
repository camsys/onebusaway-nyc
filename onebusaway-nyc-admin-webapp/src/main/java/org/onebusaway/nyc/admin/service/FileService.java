package org.onebusaway.nyc.admin.service;

import java.util.List;

public interface FileService {
  void setup();
  void setBucketName(String bucketName);
  void setGtfsPath(String gtfsPath);
  String getGtfsPath();
  void setStifPath(String stifPath);
  String getStifPath();
  void setBuildPath(String buildPath);
  String getBuildPath();
  boolean bundleDirectoryExists(String filename);

  boolean createBundleDirectory(String filename);

  List<String[]> listBundleDirectories(int maxResults);

  String get(String s3path, String tmpDir);
  List<String> list(String directory, int maxResults);
  
}
