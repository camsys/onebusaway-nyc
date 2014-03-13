package org.onebusaway.nyc.transit_data_manager.config.model.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigurationStore {

  private Map<String, ComponentConfiguration> componentMap;

  public ConfigurationStore() {
    this.componentMap = new HashMap<String, ComponentConfiguration>();
  }

  public Map<String, ComponentConfiguration> getComponentMap() {
    return componentMap;
  }

  public void setComponentMap(Map<String, ComponentConfiguration> componentMap) {
    this.componentMap = componentMap;
  }

  public List<ConfigItem> getEntireConfiguration() {
    List<ConfigItem> allConfigs = new ArrayList<ConfigItem>();

    Iterator<String> compIt = getComponentMap().keySet().iterator();

    List<ConfigItem> compConfigs = null;
    while (compIt.hasNext()) {
      String compKey = compIt.next();

      compConfigs = getComponentMap().get(compKey).getConfiguration();

      allConfigs.addAll(compConfigs);
    }

    return allConfigs;
  }

  public List<ConfigItem> getConfigForComponent(String component) {
    return getComponentMap().get(component).getConfiguration();
  }

  public ConfigItem getConfigForComponentKey(String component, String key) {
    return getComponentMap().get(component).getItemForKey(key);
  }

  public void setConfigForComponentKey(String component, String key,
      ConfigItem config) {
    
    if (getComponentMap().get(component) == null){
      getComponentMap().put(component, new ComponentConfiguration());
    }
    
    getComponentMap().get(component).setItemForKey(key, config);

  }

}
