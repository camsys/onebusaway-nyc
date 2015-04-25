package org.onebusaway.nyc.admin.model.ui;

/**
 * DTO for sending created/selected directory status to UI
 * @author abelsare
 *
 */
public class DirectoryStatus {
	private String directoryName;
	private String message;
	private boolean selected;
	private String gtfsPath;
	private String auxPath;
	private String bucketName;
	private String timestamp;

	public DirectoryStatus(String directoryName, String message, boolean selected) {
	  this(directoryName, message, selected, "");
	}

	public DirectoryStatus(String directoryName, String message, boolean selected, String timestamp) {
		this.directoryName = directoryName;
		this.message = message;
		this.selected = selected;
		this.timestamp = timestamp;
	}

	public String getDirectoryName() {
		return directoryName;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSelected() {
		return selected;
	}

	/**
	 * @return the gtfsPath
	 */
	public String getGtfsPath() {
		return gtfsPath;
	}

	/**
	 * @param gtfsPath the gtfsPath to set
	 */
	public void setGtfsPath(String gtfsPath) {
		this.gtfsPath = gtfsPath;
	}

	/**
	 * @return the auxPath for stif/hastus support
	 */
	public String getAuxPath() {
		return auxPath;
	}

	/**
	 * @param stifPath path for stif/hastus support
	 */
	public void setAuxPath(String auxPath) {
		this.auxPath = auxPath;
	}

	/**
	 * @return the bucketName
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * @param bucketName the bucketName to set
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	/**
	 * @return the timestamp
	 */
	 public String getTimestamp() {
	   return timestamp;
	 }
	 
	 /**
	  * @param timestampe the timestamp to set
	  */
	 public void setTimestamp(String timestamp) {
	   this.timestamp = timestamp;
	 }
}
