package org.onebusaway.nyc.admin.event.handler;

import org.apache.log4j.Level;
import org.onebusaway.nyc.util.logging.LoggingService;
import org.onebusaway.users.model.IndexedUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

/**
 * Calls {@link LoggingService} to log 'logout' event 
 * @author abelsare
 *
 */
public class LogoutEventListener implements ApplicationListener<HttpSessionDestroyedEvent> {
	
	private LoggingService loggingService;

	@Override
	public void onApplicationEvent(HttpSessionDestroyedEvent event) {
		SecurityContext securityContext = (SecurityContext) event.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
		IndexedUserDetails userDetails = (IndexedUserDetails) securityContext.getAuthentication().getPrincipal();
		String component = System.getProperty("admin.chefRole");
		String message = "User '" + userDetails.getUsername() + "' logged out";
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
