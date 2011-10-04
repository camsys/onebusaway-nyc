package org.onebusaway.nyc.transit_data_manager.config.model.jaxb;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ext.JodaSerializers.DateTimeSerializer;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.config.TdmIsoDateTimeNoMillisSerializer;

public class ConfigItem {
  private String component;
  private String key;
  private String value;
  private String valueType;
  private String description;
  private DateTime updated;
  
  public String getComponent() {
    return component;
  }
  public String getKey() {
    return key;
  }
  public String getValue() {
    return value;
  }
  public String getValueType() {
    return valueType;
  }
  public String getDescription() {
    return description;
  }
  
  @JsonSerialize(using=TdmIsoDateTimeNoMillisSerializer.class)
  public DateTime getUpdated() {
    return updated;
  }
  public void setComponent(String component) {
    this.component = component;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public void setValue(String value) {
    this.value = value;
  }
  public void setValueType(String valueType) {
    this.valueType = valueType;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public void setUpdated(DateTime updated) {
    this.updated = updated;
  }
  
  
}
