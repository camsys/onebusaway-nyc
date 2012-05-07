package org.onebusaway.nyc.webapp.actions.admin;

import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.impl.CreateUserAction;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Creates a user in the database. Extends {@link CreateUserAction} for customization purposes
 * if required in future.
 * @author dev
 *
 */
public class RegisterAction extends NextActionSupport {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String username;
	private String password;
	private boolean admin;
	private UserService userService;
	
	@Override
	public String execute() throws Exception {
		UserIndex userIndex = userService.getOrCreateUserForUsernameAndPassword(
		        username, password);

		    if (userIndex == null)
		      return ERROR;

		    if (admin) {
		      User user = userIndex.getUser();
		      userService.enableAdminRoleForUser(user, false);
		    }
		addActionMessage("User '" +username + "' created successfully");
		return SUCCESS;
	}
	
	/**
	 * Validates input fields
	 */
	public void validate() {
		if(username.length() == 0) {
			addFieldError("username", "User name is required");
		}
		if(password.length() == 0) {
			addFieldError("password", "Password is required");
		}
	}

	/**
	 * Returns user name of the user being created
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Injects user name of the user being created
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns password of the user being created
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Injects password of the user being created
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns true if the user is created as admin
	 * @return the admin
	 */
	public boolean isAdmin() {
		return admin;
	}

	/**
	 * Injects true if user is created as admin
	 * @param admin the admin to set
	 */
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	/**
	 * Injects {@link UserService}
	 * @param userService the userService to set
	 */
	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	

}