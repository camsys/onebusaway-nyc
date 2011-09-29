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

import java.util.Date;

import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Action for config variable page
 * 
 */
public class ConfigAction extends OneBusAwayNYCActionSupport {
  private static final long serialVersionUID = 1L;

  @Autowired
  private ConfigurationService configurationService;

  // TODO Ideally this *would* be autowired but it seems overly convoluted to
//  @Autowired
//  @Value("${systemProperties.webappApiUrl}")
//  @Value(value = "")
  private String apiUrl = System.getProperty("webapp.api.url", "");

  public int getHideTimeout() {
	  return configurationService.getConfigurationValueAsInteger("display.hideTimeout", 300);      
  }

  public int getStaleDataTimeout() {
	  return configurationService.getConfigurationValueAsInteger("display.hideTimeout", 300);      
  }

  public String getApiUrl() {
	  return apiUrl;
  }
  
  public void setApiUrl(String url) {
    apiUrl = url;
  }

  public long getEpoch() {
	  return new Date().getTime();
  }

}
