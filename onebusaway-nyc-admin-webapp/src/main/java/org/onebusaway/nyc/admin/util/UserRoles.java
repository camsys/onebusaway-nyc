package org.onebusaway.nyc.admin.util;

/**
 * Holds constants for user roles in the system
 * @author abelsare
 *
 */
public enum UserRoles {

	/** Anonymous user role **/
	ROLE_ANONYMOUS("ROLE_ANONYMOUS"),
	
	/** User/Operator user role **/
	ROLE_USER("ROLE_USER"),
	
	/** Admin user role **/
	ROLE_ADMINISTRATOR("ROLE_ADMINISTRATOR");
	
	private String role;
	
	private UserRoles(String role) {
		this.role = role;
	}
	
	public String getRole() {
		return role;
	}
}
