package org.onebusaway.nyc.admin.service;

import java.util.List;

public interface FileService {
  boolean bundleDirectoryExists(String filename);

  boolean createBundleDirectory(String filename);

  List<String[]> listBundleDirectories(int maxResults);
}
