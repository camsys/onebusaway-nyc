package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.List;

public class BundleRequest {
  private String _bundleDirectory;
  
  public String getBundleDirectory() {
    return _bundleDirectory;
  }
  
  public void setBundleDirectory(String dir) {
    _bundleDirectory = dir;
  }
}
