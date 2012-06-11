package org.onebusaway.nyc.admin.service;

import java.io.InputStream;
import java.util.List;

public interface FileService {
  void setup();
  void setS3User(String user);
  void setS3Password(String password);
  void setBucketName(String bucketName);
  void setGtfsPath(String gtfsPath);
  String getGtfsPath();
  void setStifPath(String stifPath);
  String getStifPath();
  void setBuildPath(String buildPath);
  String getBuildPath();
  String getBucketName();
  
  boolean bundleDirectoryExists(String filename);

  boolean createBundleDirectory(String filename);

  List<String[]> listBundleDirectories(int maxResults);

  String get(String s3path, String tmpDir);
  InputStream get(String s3Path);
  String put(String key, String directory);
  
  List<String> list(String directory, int maxResults);
  
  
  
}
