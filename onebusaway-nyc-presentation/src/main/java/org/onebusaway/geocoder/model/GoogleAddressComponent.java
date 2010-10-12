package org.onebusaway.geocoder.model;

import java.util.ArrayList;
import java.util.List;

public class GoogleAddressComponent {
  
  private String longName;
  private String shortName;
  private List<String> types = new ArrayList<String>();
  
  public String getLongName() {
    return longName;
  }
  public void setLongName(String longName) {
    this.longName = longName;
  }
  public String getShortName() {
    return shortName;
  }
  public void setShortName(String shortName) {
    this.shortName = shortName;
  }
  public void addType(String type) {
    this.types.add(type);
  }
  public List<String> getTypes() {
    return this.types;
  }
}
