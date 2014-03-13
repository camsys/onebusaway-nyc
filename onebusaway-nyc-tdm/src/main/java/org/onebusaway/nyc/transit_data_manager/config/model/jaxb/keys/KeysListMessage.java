package org.onebusaway.nyc.transit_data_manager.config.model.jaxb.keys;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;

@XmlRootElement
public class KeysListMessage extends Message {
  private List<Key> keys;
  
  public List<Key> getKeys() {
    return keys;
  }
  
  public void setKeys(List<Key> keys) {
    this.keys = keys;
  }
  
}
