package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.users.services.CurrentUserService;

import org.springframework.beans.factory.annotation.Autowired;
/**
 * Make the user service available to subclasses, and provide convenience
 * check to test authorization. 
 *
 */
public class AuthenticatedResource {

  private CurrentUserService _currentUserService;
  
  @Autowired
  public void setCurrentUserService(CurrentUserService userDataService) {
    _currentUserService = userDataService;
  }

  protected boolean isAuthorized() {
    return _currentUserService.isCurrentUserAdmin();
  }

}
