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
