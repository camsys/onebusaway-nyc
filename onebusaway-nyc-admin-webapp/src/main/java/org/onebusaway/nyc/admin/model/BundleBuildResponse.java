package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleBuildResponse {
  private List<String> _gtfsList = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _stifZipList = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _statusList = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _outputFileList = Collections.synchronizedList(new ArrayList<String>());
  private Exception _exception = null;
  private boolean _isComplete = false;
  private String _bundleRootDirectory;
  private String _bundleInputDirectory;
  private String _bundleOutputDirectory;
  private String _bundleDataDirectory;
  private String _bundleTarFilename;
  private String _remoteInputDirectory;
  private String _remoteOutputDirectory;
  private String _versionString;
  private String _tmpDirectory;
  private String _bundleBuildName;

  private String _id = null;
  private String bundleResultLink;

  // no arg constructor for serialization
  public BundleBuildResponse() {
    
  }
  public BundleBuildResponse(String id) {
    _id = id;
  }

  public String toString() {
    return "BundleBuildResponse{[" + _id + "], bundleResultLink=" + bundleResultLink
        + ", statusList=" + _statusList 
        + ", complete=" + _isComplete + "}"; 
  }

  public String getId() {
    return _id;
  }

  public void setId(String id) {
    _id = id;
  }

  public void addGtfsFile(String file) {
    _gtfsList.add(file);
  }

  public List<String> getGtfsList() {
    return new ArrayList<String>(_gtfsList);
  }

  // for JSON serialization only
  public void setGtfsList(List<String> gtfsList) {
    _gtfsList = gtfsList;
  }
  
  public void addStifZipFile(String file) {
    _stifZipList.add(file);
  }

  public List<String> getStifZipList() {
    return new ArrayList<String>(_stifZipList);
  }

  // for JSON serializaton only
  public void setStifZipList(List<String> stifZipList) {
    _stifZipList = stifZipList;
  }
  
  public void addStatusMessage(String msg) {
    _statusList.add(msg);
  }

  public List<String> getStatusList() {
    return new ArrayList<String>(_statusList);
  }

  // for JSON serialization only
  public void setStatusList(List<String> statusList) {
    _statusList = statusList;
  }
  
  public void addOutputFile(String name) {
    _outputFileList.add(name);
  }
  
  public List<String> getOutputFileList() {
    return new ArrayList<String>(_outputFileList);
  }

  public void setOutputFileList(List<String> outputFileList) {
    _outputFileList = outputFileList;
  }
  
  public void addException(Exception e) {
    _exception = e;
  }
  
  public void setException(Exception e) {
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

  public String getBundleDataDirectory() {
    return _bundleDataDirectory;
  }
  public void setBundleDataDirectory(String directoryPath) {
    _bundleDataDirectory = directoryPath;
  }

  public String getBundleInputDirectory() {
    return _bundleInputDirectory;
  }
  public void setBundleInputDirectory(String directoryPath) {
    _bundleInputDirectory = directoryPath;
    
  }

  public String getBundleTarFilename() {
    return _bundleTarFilename;
  }
  
  public void setBundleTarFilename(String filename) {
    _bundleTarFilename = filename;
  }

  public String getBundleRootDirectory() {
    return _bundleRootDirectory;
  }
  
  public void setBundleRootDirectory(String directoryPath) {
    _bundleRootDirectory = directoryPath;
  }

  public String getTmpDirectory() {
    return _tmpDirectory;
  }
  
  public void setTmpDirectory(String tmpDirectory) {
    _tmpDirectory = tmpDirectory;
  }

  public String getRemoteInputDirectory() {
    return _remoteInputDirectory;
  }
  
  public void setRemoteInputDirectory(String directoryPath) {
    _remoteInputDirectory = directoryPath;
  }

  public String getRemoteOutputDirectory() {
    return _remoteOutputDirectory;
  }

   public void setRemoteOutputDirectory(String directoryPath) {
    _remoteOutputDirectory = directoryPath;
  }

	/**
	 * @return the bundleResultLink
	 */
	public String getBundleResultLink() {
		return bundleResultLink;
	}
	
	/**
	 * @param bundleResultLink the bundleResultLink to set
	 */
	public void setBundleResultLink(String bundleResultLink) {
		this.bundleResultLink = bundleResultLink;
	}

	public String getBundleBuildName() {
	  return _bundleBuildName;
	}
	
  public void setBundleBuildName(String bundleName) {
    this._bundleBuildName = bundleName;
  }

}
