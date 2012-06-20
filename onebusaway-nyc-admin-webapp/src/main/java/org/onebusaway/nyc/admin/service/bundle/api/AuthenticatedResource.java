package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.users.services.CurrentUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Make the user service available to subclasses, and provide convenience
 * check to test authorization. 
 *
 */
public class AuthenticatedResource {

  private static Logger _log = LoggerFactory.getLogger(AuthenticatedResource.class);
  private CurrentUserService _currentUserService;
  
  @Autowired
  public void setCurrentUserService(CurrentUserService userDataService) {
    _currentUserService = userDataService;
  }

  protected boolean isAuthorized() {
    boolean isAuthorized = _currentUserService.isCurrentUserAdmin();
    if (!isAuthorized) {
      _log.info("request not authorized");
    }
    return isAuthorized;
  }

}
