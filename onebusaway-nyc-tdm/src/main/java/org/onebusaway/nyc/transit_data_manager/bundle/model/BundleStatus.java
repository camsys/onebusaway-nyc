package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleStatus implements Serializable {


  private static final long serialVersionUID = 1L;
  public static final String STATUS_STARTED = "in_progress";
  public static final String STATUS_COMPLETE = "complete";
  public static final String STATUS_ERROR = "error";
  public static final String STATUS_STAGING_COMPLETE = "staging_complete";
  
  private String id = null;
  private String status = "initialized";
  private List<String> bundleNames = Collections.synchronizedList(new ArrayList<String>());
  
  public List<String> getBundleNames() {
    return new ArrayList<String>(bundleNames);
  }
  
  public void setBundleNames(List<String> bundleNames) {
    this.bundleNames = bundleNames;
  }
  
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }

  public void addBundleName(String bundleFilename) {
	bundleNames.add(bundleFilename);
  }

}
