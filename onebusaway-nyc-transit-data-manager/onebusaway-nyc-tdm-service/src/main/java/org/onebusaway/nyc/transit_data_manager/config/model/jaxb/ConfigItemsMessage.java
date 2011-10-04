package org.onebusaway.nyc.transit_data_manager.config.model.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigItemsMessage extends Message {
  private List<ConfigItem> config;
  
  public List<ConfigItem> getConfig() {
    return config;
  }
  
  public void setConfig(List<ConfigItem> config) {
    this.config = config;
  }
  
}
