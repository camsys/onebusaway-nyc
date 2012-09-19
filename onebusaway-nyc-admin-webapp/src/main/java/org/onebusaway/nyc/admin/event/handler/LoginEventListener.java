package org.onebusaway.nyc.admin.event.handler;

import org.apache.log4j.Level;
import org.onebusaway.nyc.util.logging.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.event.authentication.AuthenticationSuccessEvent;
import org.springframework.security.userdetails.UserDetails;

/**
 * Listens to @{link AuthenticationSuccessEvent} and calls logging service to log successful login event
 * @author abelsare
 *
 */
public class LoginEventListener implements ApplicationListener<AuthenticationSuccessEvent>{

	private LoggingService loggingService;
	
	@Override
	public void onApplicationEvent(AuthenticationSuccessEvent event) {
		UserDetails userDetails = (UserDetails) event.getAuthentication().getPrincipal();
		String component = System.getProperty("admin.chefRole");
		String message = "User '" + userDetails.getUsername() + "' logged in";
		loggingService.log(component, Level.INFO, message);
	}

	/**
	 * @param loggingService the loggingService to set
	 */
	@Autowired
	public void setLoggingService(LoggingService loggingService) {
		this.loggingService = loggingService;
	}
	

}
