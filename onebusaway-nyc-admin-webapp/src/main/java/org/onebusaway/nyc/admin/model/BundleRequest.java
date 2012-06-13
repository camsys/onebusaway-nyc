package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.List;

public class BundleRequest {
  private String _bundleDirectory;
  private String _tmpDirectory;
  private String _bundleBuildName;
  
  public String getBundleDirectory() {
    return _bundleDirectory;
  }
  
  public void setBundleDirectory(String dir) {
    _bundleDirectory = dir;
  }

  public String getTmpDirectory() {
    return _tmpDirectory;
  }
  
  public void setTmpDirectory(String dir) {
    _tmpDirectory = dir;
  }

  public String getBundleBuildName() {
    return _bundleBuildName;
  }
  
  public void setBundleBuildName(String bundleName) {
    this._bundleBuildName = bundleName;
  }
}
