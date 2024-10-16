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

package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.struts2.convention.annotation.AllowedMethods;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.model.ui.DirectoryStatus;
import org.onebusaway.nyc.admin.model.ui.ExistingDirectory;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;


/**
 * Action class that holds properties and methods required across all bundle building UI pages
 * @author abelsare
 * @author sheldonabrown
 *
 */
@Namespace(value="/admin/bundles")
@Results({
    @Result(type = "redirectAction", name = "redirect", params = {
    "actionName", "manage-bundles"}),
    @Result(name="selectDirectory", type="json", 
  params={"root", "directoryStatus"}),
    @Result(name="validationResponse", type="json", 
  params={"root", "bundleResponse"}),
    @Result(name="buildResponse", type="json", 
  params={"root", "bundleBuildResponse"}),
		@Result(name="existingBuildList", type="json",
				params={"root", "existingBuildList"}),
    @Result(name="fileList", type="json", 
  params={"root", "fileList"}),
  	@Result(name="downloadZip", type="stream", 
  params={"contentType", "application/zip", 
          "inputName", "downloadInputStream",
          "contentDisposition", "attachment;filename=\"output.zip\"",
          "bufferSize", "1024"}),
    @Result(name="download", type="stream", 
  params={"contentType", "text/html", 
        "inputName", "downloadInputStream",
        "contentDisposition", "attachment;filename=\"${downloadFilename}\"",
        "bufferSize", "1024"})
})
@AllowedMethods({"selectDirectory", "copyDirectory", "deleteDirectory", "createDirectory",
		"fileList", "updateBundleComments", "existingBuildList", "download", "buildList",
		"buildOutputZip", "downloadOutputFile", "downloadBundle", "downloadValidateFile"})
public class ManageBundlesAction extends OneBusAwayNYCAdminActionSupport implements ServletContextAware {
  private static Logger _log = LoggerFactory.getLogger(ManageBundlesAction.class);
	private static final long serialVersionUID = 1L;
	//To hold the final directory name 
	private String bundleDirectory;
	//Holds the value entered in the text box
	private String directoryName;
	// what to call the bundle, entered in the text box
	private String bundleName;
	private boolean productionTarget;
	private String comments;
	private FileService fileService;
	private BundleRequestService bundleRequestService;
	private static final int MAX_RESULTS = -1;
	private BundleResponse bundleResponse;
	private BundleBuildResponse bundleBuildResponse;
	private String id;
	private String downloadFilename;
	private String emailTo;
	private InputStream downloadInputStream;
	private List<String> fileList = new ArrayList<String>();
	private String selectedBundleName;
	private DirectoryStatus directoryStatus;
	// where the bundle is deployed to
	private String s3Path = "s3://bundle-data/activebundle/<env>/";
	private String environment = "dev";
	private SortedMap<String, String> existingBuildList = new TreeMap<String, String>();
	
	@Override
	public String input() {
	  _log.debug("in input");
    return SUCCESS;
	  
	}
	
	@Override
	public String execute() {
	  _log.info("in execute");
	  return SUCCESS;
	}
	
	/**
	 * Creates directory for uploading bundles on AWS
	 */
	public String createDirectory() {
	  String createDirectoryMessage = null;
	  boolean directoryCreated = false;
	  
	  _log.debug("in create directory with dir=" + directoryName);
		if(fileService.bundleDirectoryExists(directoryName)) {
		  _log.info("bundle dir exists");
			createDirectoryMessage = directoryName + " already exists. Please try again!";
		} else {
		  _log.info("creating bundledir=" + directoryName);
			//Create the directory if it does not exist.
			directoryCreated = fileService.createBundleDirectory(directoryName);
			bundleDirectory = directoryName;
			if(directoryCreated) {
				createDirectoryMessage = "Successfully created new directory: " +directoryName;
			} else {
				createDirectoryMessage = "Unable to create direcory: " +directoryName;
			}
		}
		 
		directoryStatus = createDirectoryStatus(createDirectoryMessage, directoryCreated);
		return "selectDirectory";
	}
	
	public String selectDirectory() {
	  _log.info("in selectDirectory with dirname=" + directoryName);
	  bundleDirectory = directoryName;
	  directoryStatus = createDirectoryStatus("Loaded existing directory " + directoryName, true);
	  return "selectDirectory";
	}
	
	private DirectoryStatus createDirectoryStatus(String statusMessage, boolean selected) {
		DirectoryStatus directoryStatus = new DirectoryStatus(directoryName, statusMessage, selected);
		directoryStatus.setGtfsPath(fileService.getGtfsPath());
		directoryStatus.setStifPath(fileService.getStifPath());
		directoryStatus.setBucketName(fileService.getBucketName());
		return directoryStatus;
	}
	
	/**
	 * Returns the existing directories in the current bucket on AWS
	 * @return list of existing directories
	 */
	public List<ExistingDirectory> getExistingDirectories() {
		List<String[]> existingDirectories = fileService.listBundleDirectories(MAX_RESULTS);
		List<ExistingDirectory> directories = new ArrayList<ExistingDirectory>();
		for(String[] existingDirectory : existingDirectories) {
			ExistingDirectory directory = new ExistingDirectory(existingDirectory[0], existingDirectory[1], 
					existingDirectory[2]);
			directories.add(directory);
		}
		
		return directories;
	}
	
	public String fileList() {
	  _log.info("fileList called for id=" + id); 
	  this.bundleResponse = bundleRequestService.lookupValidationRequest(getId());
	  fileList.clear();
	  if (this.bundleResponse != null) {
	    fileList.addAll(this.bundleResponse.getValidationFiles());
	  }
	  return "fileList";
	}

	public String existingBuildList() {
		existingBuildList.clear();
		_log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ selectedBundleName +"/"+fileService.getBuildPath());
		List<String> existingDirectories = fileService.listBundleBuilds( selectedBundleName +"/" +fileService.getBuildPath()+"/" , MAX_RESULTS);
		if(existingDirectories == null){
			return null;
		}
		int i = 1;
		for(String directory: existingDirectories) {
			String[] buildSplit = directory.split("/");
			existingBuildList.put(buildSplit[buildSplit.length-1], ""+i++);
		}

		return "existingBuildList";
	}


	
	public String download() {
	  this.bundleResponse = bundleRequestService.lookupValidationRequest(getId());
	  _log.info("download=" + this.downloadFilename + " and id=" + id);
	  if (this.bundleResponse != null) {
	    this.downloadInputStream = new FileUtils().read(this.bundleResponse.getTmpDirectory() + File.separator + this.downloadFilename);
	    return "download";
	  }
	  // TODO
	  _log.error("bundleResponse not found for id=" + id);
	  return "error";
	}

	public String buildList() {
	  _log.info("buildList called with id=" + id);
	  this.bundleBuildResponse = this.bundleRequestService.lookupBuildRequest(getId());
	  if (this.bundleBuildResponse != null) {
	    fileList.addAll(this.bundleBuildResponse.getOutputFileList());
	  }
	  return "fileList";
	}
	
	public String buildOutputZip() {
		_log.info("buildOuputZip called with id=" +id);
		bundleBuildResponse = bundleRequestService.lookupBuildRequest(getId());
		String zipFileName = fileService.createOutputFilesZip(bundleBuildResponse.getRemoteOutputDirectory());
		try {
			downloadInputStream = new FileInputStream(zipFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "downloadZip";
	}
	
	public String downloadOutputFile() {
	  _log.info("downloadOutputFile with id=" + id + " and file=" + this.downloadFilename);
	  fileService.validateFileName(downloadFilename);
	  try {
		  this.bundleBuildResponse = this.bundleRequestService.lookupBuildRequest(getId());
	  } catch(Throwable t){
	  	_log.error("transaction issue " + t, t);
	  }

	  if (this.bundleBuildResponse != null) {
	    String s3Key = bundleBuildResponse.getRemoteOutputDirectory() + File.separator + this.downloadFilename;
	    _log.info("get request for s3Key=" + s3Key);
	    this.downloadInputStream = this.fileService.get(s3Key);
	    return "download";
	  }
	  // TODO
	  return "error";
	}

	public String downloadValidateFile() {
	  this.bundleResponse = this.bundleRequestService.lookupValidationRequest(getId());
	  fileService.validateFileName(downloadFilename);
	  if (this.bundleResponse != null) {
	    String s3Key = bundleResponse.getRemoteOutputDirectory() + File.separator + this.downloadFilename;
	    _log.info("get request for s3Key=" + s3Key);
	    this.downloadInputStream = this.fileService.get(s3Key);
	    return "download";
	  }
	  // TODO
	  _log.error("validate file not found for id=" + getId());
	  return "error";
	}
	
	/**
	 * @return the directoryName
	 */
	public String getDirectoryName() {
		return directoryName;
	}
	
	/**
	 * @param directoryName the directoryName to set
	 */
	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}
	
	/**
	 * @return the productionTarget
	 */
	public boolean isProductionTarget() {
		return productionTarget;
	}
	
	/**
	 * @param productionTarget the productionTarget to set
	 */
	public void setProductionTarget(boolean productionTarget) {
		this.productionTarget = productionTarget;
	}
	
	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}
	
	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	/**
	 * @param fileService the fileService to set
	 */
	@Autowired
	public void setFileService(FileService fileService) {
		this.fileService = fileService;
	}

	/**
	 * @return the bundleDirectory
	 */
	public String getBundleDirectory() {
		return bundleDirectory;
	}
	
	/**
	 * @param bundleDirectory the bundleDirectory to set
	 */
	public void setBundleDirectory(String bundleDirectory) {
		this.bundleDirectory = bundleDirectory;
	}
	

	/**
	 * Injects {@link BundleRequestService}
	 * @param bundleRequestService the bundleRequestService to set
	 */
	@Autowired
	public void setBundleRequestService(BundleRequestService bundleRequestService) {
		this.bundleRequestService = bundleRequestService;
	}

	public BundleResponse getBundleResponse() {
	  return bundleResponse;
	}

	public BundleBuildResponse getBundleBuildResponse() {
	  return bundleBuildResponse;
	}
	
	public void setId(String id) {
	  this.id = id;
	}
	
	public String getId() {
	    return id;
	}
	
	public void setBundleName(String bundleName) {
	  this.bundleName = bundleName;
	}
	
	public String getBundleName() {
    return bundleName;
	}

	public void setSelectedBundleName(String selectedBundleName) {
		this.selectedBundleName = selectedBundleName;
	}

	public String getSelectedBundleName() {
		return selectedBundleName;
	}

	public DirectoryStatus getDirectoryStatus() {
	  return directoryStatus;
	}
	
	public InputStream getDownloadInputStream() {
	  return this.downloadInputStream;
	}
	
	public void setDownloadFilename(String name) {
	  this.downloadFilename = name;
	}
	
	public String getDownloadFilename() {
	  return this.downloadFilename;
	}

	public List<String> getFileList() {
	  return this.fileList;
	}

	public SortedMap<String, String> getExistingBuildList() {
		return this.existingBuildList;
	}

	public void setEmailTo(String to) {
	  emailTo = to;
	}

	public String getS3Path() {
	  return s3Path;
	}

	public String getEnvironment() {
	  return environment;
	}


	@Override
  public void setServletContext(ServletContext context) {
	    if (context != null) {
	      String obanycEnv = context.getInitParameter("obanyc.environment");
	      if (obanycEnv != null && obanycEnv.length() > 0) {
	        environment = obanycEnv;
	        s3Path = "s3://" + context.getInitParameter("s3.bundle.bucketName")
	          + "/activebundles/" + environment
	          + "/";
	        _log.info("injecting env=" + environment + ", s3Path=" + s3Path);
	      }
	    }
  }
}
