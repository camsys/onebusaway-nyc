package org.onebusaway.nyc.admin.service;

import java.util.List;

public interface FileService {
  void setup();
  void setBucketName(String bucketName);
  
  boolean bundleDirectoryExists(String filename);

  boolean createBundleDirectory(String filename);

  List<String[]> listBundleDirectories(int maxResults);

  String get(String s3path, String tmpDir);
}
