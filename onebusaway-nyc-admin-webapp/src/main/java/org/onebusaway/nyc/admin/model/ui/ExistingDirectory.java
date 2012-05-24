package org.onebusaway.nyc.admin.model.ui;

/**
 * DTO for sending existing directory to the UI
 * @author abelsare
 *
 */
public class ExistingDirectory {
	private String name;
	private String type;
	private String creationTimestamp;
	
	
	public ExistingDirectory(String name, String type, String creationTimestamp) {
		super();
		this.name = name;
		this.type = type;
		this.creationTimestamp = creationTimestamp;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the creationTimestamp
	 */
	public String getCreationTimestamp() {
		return creationTimestamp;
	}
	/**
	 * @param creationTimestamp the creationTimestamp to set
	 */
	public void setCreationTimestamp(String creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}
	
	
}