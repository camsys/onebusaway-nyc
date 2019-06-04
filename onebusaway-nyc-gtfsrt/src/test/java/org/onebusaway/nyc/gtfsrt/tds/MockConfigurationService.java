/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.tds;

import org.onebusaway.nyc.util.configuration.ConfigurationService;

import java.util.Map;

/**
 * Mock out configuration service: simply return the defaults.
 */
public class MockConfigurationService implements ConfigurationService {
  @Override
  public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
    return defaultValue;
  }

  @Override
  public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
    return defaultValue;
  }

  @Override
  public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
    return defaultValue;
  }

  @Override
  public Boolean getConfigurationValueAsBoolean(String configurationItemKey, Boolean defaultValue) {
    return defaultValue;
  }

  @Override
  public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
    throw new IllegalArgumentException("not implemented");
  }

  @Override
  public Map<String, String> getConfiguration() {
    throw new IllegalArgumentException("not implemented");
  }
}
