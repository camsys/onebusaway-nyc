package org.onebusaway.nyc.presentation.impl;

/**
 * Utility to remove the agency id from an entity id
 */
public class WebappIdParser {
  
  public String parseIdWithoutAgency(String id) {
    if (id == null) throw new NullPointerException("id is null");
    id = id.trim();
    String[] fields = id.split("_", 2);
    if (fields.length != 2) throw new IllegalArgumentException("'" + id + "' does not look like an id with an agency");
    return fields[1];    
  }

}
