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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.config.DateTimeXmlAdapter;
import org.onebusaway.nyc.transit_data_manager.config.TdmIsoDateTimeNoMillisSerializer;

public class ConfigItem {
  private String component;
  private String key;
  private String value;
  private String units;
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
  
  @XmlJavaTypeAdapter(type=DateTime.class, value=DateTimeXmlAdapter.class)
  @JsonSerialize(using=TdmIsoDateTimeNoMillisSerializer.class)
  public DateTime getUpdated() {
    return updated;
  }
  
  public String getUnits() {
    return units;
  }
  public void setUnits(String units) {
    this.units = units;
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
