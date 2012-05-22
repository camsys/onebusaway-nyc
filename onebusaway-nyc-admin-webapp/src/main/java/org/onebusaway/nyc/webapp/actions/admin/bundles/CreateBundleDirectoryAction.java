package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller for create directory UI. Uses {@link FileService} for directory operations.
 * @author abelsare
 *
 */
@Results({@Result(type = "redirectAction", name = "redirect", params = {
	     "actionName", "create-bundle-directory"})})
public class CreateBundleDirectoryAction extends ManageBundlesAction {
	
	private static final long serialVersionUID = 1L;
	private String createDirectoryMessage;
	private String directoryName;
	private boolean directoryCreated;
	private boolean productionTarget;
	private String comments;
	private FileService fileService;
	private static final int MAX_RESULTS = 10;
	
	
	/**
	 * Creates directory for uploading bundles on AWS
	 */
	public void createDirectory() {
		if(fileService.bundleDirectoryExists(directoryName)) {
			createDirectoryMessage = directoryName + " already exists. Please try again!";
		} else {
			//Create the directory if it does not exist.
			directoryCreated = fileService.createBundleDirectory(directoryName);
			selectDirectory();
			if(directoryCreated) {
				createDirectoryMessage = "Successfully created new directory: " +directoryName;
			} else {
				createDirectoryMessage = "Unable to create direcory: " +directoryName;
			}
		}
	}
	
	/**
	 * Returns the existing directories in the current bucket on AWS
	 * @return list of existing directories
	 */
	public List<String[]> getExistingDirectories() {
		return fileService.listBundleDirectories(MAX_RESULTS);
	}
	
	/**
	 * Stores the newly created or selected directory name from the UI.
	 */
	public void selectDirectory() {
		bundleDirectory = directoryName;
	}
	
	/**
	 * @return the createDirectoryMessage
	 */
	public String getCreateDirectoryMessage() {
		return createDirectoryMessage;
	}
	
	/**
	 * @param createDirectoryMessage the createDirectoryMessage to set
	 */
	public void setCreateDirectoryMessage(String createDirectoryMessage) {
		this.createDirectoryMessage = createDirectoryMessage;
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
	 * @return the directoryCreated
	 */
	public boolean isDirectoryCreated() {
		return directoryCreated;
	}
	
	/**
	 * @param directoryCreated the directoryCreated to set
	 */
	public void setDirectoryCreated(boolean directoryCreated) {
		this.directoryCreated = directoryCreated;
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
	//@Autowired
	public void setFileService(FileService fileService) {
		this.fileService = fileService;
	}

	
	

}
