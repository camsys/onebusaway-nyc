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

package org.onebusaway.nyc.transit_data_manager.config.model.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ComponentConfiguration {
  private Map<String, ConfigItem> configMap;
  
  public ComponentConfiguration() {
    this.configMap = new HashMap<String, ConfigItem>();
  }

  public Map<String, ConfigItem> getConfigMap() {
    return configMap;
  }

  public void setConfigMap(Map<String, ConfigItem> configMap) {
    this.configMap = configMap;
  }

  public List<ConfigItem> getConfiguration() {
    List<ConfigItem> configuration = new ArrayList<ConfigItem>();
    
    Iterator<String> keyIt = getConfigMap().keySet().iterator();
    
    while (keyIt.hasNext()) {
      configuration.add(getConfigMap().get(keyIt.next()));      
    }
    
    return configuration;
  }
  
  public ConfigItem getItemForKey(String key) {
    return getConfigMap().get(key);
  }

  public void setItemForKey(String key, ConfigItem config) {
    getConfigMap().put(key, config);
  }
  
}
