package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.List;

public class BundleRequest {
  private List<String> _gtfsList = new ArrayList<String>();
  
  public List<String> getGtfsList() {
    return _gtfsList;
  }
}
