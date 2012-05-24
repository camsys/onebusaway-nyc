package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleBuildResponse {
  private List<String> _gtfsList = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _stifZipList = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _statusList = Collections.synchronizedList(new ArrayList<String>());
  private Exception _exception = null;
  private boolean _isComplete = false;
  private String _bundleOutputDirectory;
  private String _versionString;

  private String _id = null;

  public BundleBuildResponse(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  public void addGtfsFile(String file) {
    _gtfsList.add(file);
  }

  public List<String> getGtfsList() {
    return new ArrayList(_gtfsList);
  }

  public void addStifZipFile(String file) {
    _stifZipList.add(file);
  }

  public List<String> getStifZipList() {
    return new ArrayList(_stifZipList);
  }

  public void addStatusMessage(String msg) {
    _statusList.add(msg);
  }

  public List<String> getStatusList() {
    return new ArrayList(_statusList);
  }

  public void addException(Exception e) {
    _exception = e;
  }

  public Exception getException() {
    return _exception;
  }

  public void setComplete(boolean complete) {
    _isComplete = complete;
  }

  public boolean isComplete() {
    return _isComplete;
  }

  public void setBundleOutputDirectory(String bundleDir) {
    _bundleOutputDirectory = bundleDir;
  }

  public String getBundleOutputDirectory() {
    return _bundleOutputDirectory;
  }

  public void setVersionString(String versionString) {
    _versionString = versionString;
  }

  public String getVersionString() {
    return _versionString;
  }

}
