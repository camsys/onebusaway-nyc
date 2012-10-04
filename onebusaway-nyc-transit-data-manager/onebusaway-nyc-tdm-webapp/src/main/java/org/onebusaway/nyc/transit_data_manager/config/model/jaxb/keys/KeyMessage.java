package org.onebusaway.nyc.transit_data_manager.config.model.jaxb.keys;

import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;

@XmlRootElement
public class KeyMessage extends Message {
  private Key key;

  public Key getKey() {
    return key;
  }

  public void setKey(Key key) {
    this.key = key;
  }
  
}
