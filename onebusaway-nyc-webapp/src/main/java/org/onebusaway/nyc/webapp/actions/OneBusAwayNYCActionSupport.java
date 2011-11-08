/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions;

import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.client.model.UserBean;
import org.onebusaway.users.services.CurrentUserService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Abstract class that is currently being used to hang stub data methods onto
 */
public abstract class OneBusAwayNYCActionSupport extends NextActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  protected CurrentUserService _currentUserService;
  
  public boolean isAdminUser() {
	return _currentUserService.isCurrentUserAdmin();
  }
  
  public boolean isAnonymousUser() {
    return _currentUserService.isCurrentUserAnonymous();
  }

  public void setSession(Map<String, Object> session) {
    _session = session;
  }
  
  @Autowired
  public void setCurrentUserService(CurrentUserService currentUserService) {
    _currentUserService = currentUserService;
  }

  protected UserBean getCurrentUser() {
    UserBean user = _currentUserService.getCurrentUser();
    if (user == null)
      user = _currentUserService.getAnonymousUser();
    return user;
  }

}
