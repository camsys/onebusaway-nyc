/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleBuildResponse {
	private List<String> _gtfsList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> _stifZipList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> _transformationList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> _configList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> _statusList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> _outputFileList = Collections.synchronizedList(new ArrayList<String>());
	private SerializableException _exception = null;
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
	private String bundleStartDate;
	private String bundleEndDate;


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
				+ ", complete=" + _isComplete
				+ ", exception=" + _exception + "}"; 
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
	public void addTransformationFile(String file) {
		_transformationList.add(file);
	}

	public List<String> getTransformationList() {
		return new ArrayList<String>(_transformationList);
	}

	// for JSON serializaton only
	public void setTransformationList(List<String> transformationList) {
		_transformationList = transformationList;
	}

	public List<String> getConfigList() {
	  return new ArrayList<String>(_configList);
	}
	
	public void setConfigList(List<String> configList) {
	  _configList = configList;
	}
	
  public void addConfigFile(String file) {
    _configList.add(file);
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

	/**
	 * This method is additive, it chains exceptions if called multiple times.
	 */
	public void setException(Exception e) {
	  if (e == null) {
	    _exception = null;
	    return;
	  }
	  if (_exception== null) {
	  _exception = new SerializableException(e.getMessage(), e);
	  } else {
	    _exception = new SerializableException(e, _exception);
	  }
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
	 /**
	  * @return the bundleStartDate
	  */
	 public String getBundleStartDate() {
		 return bundleStartDate;
	 }
	 /**
	  * @param bundleStartDate the bundleStartDate to set
	  */
	 public void setBundleStartDate(String bundleStartDate) {
		 this.bundleStartDate = bundleStartDate;
	 }
	 /**
	  * @return the bundleEndDate
	  */
	 public String getBundleEndDate() {
		 return bundleEndDate;
	 }
	 /**
	  * @param bundleEndDate the bundleEndDate to set
	  */
	 public void setBundleEndDate(String bundleEndDate) {
		 this.bundleEndDate = bundleEndDate;
	 }

	 
	 private static class SerializableException extends Exception implements Serializable {
		 private String _msg = "";
		 private String _rootCause = "";

		 public SerializableException(String msg, Exception rootCause) {
			 _msg = rootCause.getClass().getName() + ":" + msg;
			 int count = 0;
			 for (StackTraceElement ste:rootCause.getStackTrace()) {
				 _rootCause += ste.toString() + "\n";
				 count++;
				 if (count > 1) break;
			 }
		 }

		 public SerializableException(Exception newException, Exception rootCause) {
			 _msg = newException.getClass().getName() + ":"  + newException.getMessage() 
					 + ";  " + rootCause.getClass().getName() + ":" + rootCause.getMessage();

			 int count = 0;
			 for (StackTraceElement ste:newException.getStackTrace()) {
				 _rootCause += ste.toString() + "\n";
				 count++;
				 if (count > 1) break;
			 }
			 _rootCause += "\n\n  Caused by:\n\n";
			 count++;
			 for (StackTraceElement ste:rootCause.getStackTrace()) {
				 _rootCause += ste.toString() + "\n";
				 if (count > 1) break;
			 }
		 }

		 public String getMessage() {
			 return toString();
		 }

		 public String toString() {
			 return  _msg + "\n  Caused by:\n\n" + _rootCause; 
		 }
		
	 }

}
