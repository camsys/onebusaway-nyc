/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.webapp.actions;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.client.model.UserBean;
import org.onebusaway.users.services.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class that is currently being used to hang stub data methods onto
 */
public abstract class OneBusAwayNYCActionSupport extends NextActionSupport {

  private static final long serialVersionUID = 1L;

  private Long time = null;
  
  public void setTime(long time) {
    this.time = time;
  }
  
  public long getTime() {
    if(time != null) {
      return time;
    } else {
      return System.currentTimeMillis();
    }
  }
  
  @Autowired
  protected CurrentUserService _currentUserService;

  @Autowired
  private ConfigurationService _configurationService;
  
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

  // hide or show MTA "weekender" link
  public boolean getShowWeekender() {
    Calendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(getTime());

    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    
    switch(dayOfWeek) {
      case Calendar.SATURDAY:
        return true;
      case Calendar.SUNDAY:
        return true;
      case Calendar.FRIDAY:
        if(hour >= 15) {
          return true;
        }                
      case Calendar.MONDAY:
        if(hour < 5) {
          return true;
        }        
    }
    
    return false;
  }
  
  // CSS + JS cache busting parameter
  public String getFrontEndVersion() {
	 return System.getProperty("front-end.version");
  }

  public String getCacheBreaker() {
	  return String.valueOf(System.currentTimeMillis());
  }

  protected UserBean getCurrentUser() {
    UserBean user = _currentUserService.getCurrentUser();
    if (user == null)
      user = _currentUserService.getAnonymousUser();
    return user;
  }

  public String getSurveyText() {
    return _configurationService.getConfigurationValueAsString(
            "display.surveyTextMobile", "Take a brief survey to help us improve your ride");
  }

  public String getSurveyLinkText() {
    return _configurationService.getConfigurationValueAsString(
            "display.surveyLinkTextMobile", "mta.info/csurvey");
  }

  public String getSurveyLinkUrl() {
    return _configurationService.getConfigurationValueAsString(
            "display.surveyLinkUrlMobile", "https://mta.info/csurvey");
  }

  public Boolean getShowSurvey() {
    return _configurationService.getConfigurationValueAsBoolean(
            "display.showSurveyMobile", false);
  }

}
