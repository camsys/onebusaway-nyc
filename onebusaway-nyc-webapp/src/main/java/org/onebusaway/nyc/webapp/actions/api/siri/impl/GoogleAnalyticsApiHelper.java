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

package org.onebusaway.nyc.webapp.actions.api.siri.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class GoogleAnalyticsApiHelper {

  @Autowired
  private ConfigurationService _config;

  public GoogleAnalyticsApiHelper(ConfigurationService configurationService) {
    this._config = configurationService;
  }
  
  public boolean reportToGoogleAnalytics(final String key) {
    if ((key == null) || key.isEmpty())
      return false;
    String[] excludeKeys = StringUtils.split(_config.getConfigurationValueAsString("display.obaApiExcludeGaKey", "OBANYC"), ",");
    for (String excludeKey: excludeKeys) {
      if (StringUtils.equals(excludeKey, key)) 
        return false;
    }
    return true;
  }

}
