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

import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action for config variable page
 * 
 */
public class ConfigAction extends OneBusAwayNYCActionSupport {
  private static final long serialVersionUID = 1L;

  @Autowired
  private NycConfigurationService configurationService;

  public int getHideTimeout() {
	  ConfigurationBean config = configurationService.getConfiguration();
	  return config.getHideTimeout();
  }

  public int getStaleDataTimeout() {
      ConfigurationBean config = configurationService.getConfiguration();
      return config.getStaleDataTimeout();
  }

  public String getApiUrl() {
      ConfigurationBean config = configurationService.getConfiguration();
      return config.getApiUrl();
  }

  public long getEpoch() {
	  return new Date().getTime();
  }

}
