package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigDeployStatus implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String STATUS_STARTED = "in_progress";
  public static final String STATUS_COMPLETE = "complete";
  public static final String STATUS_ERROR = "error";
  public static final String STATUS_STAGING_COMPLETE = "staging_complete";

  private String id = null;
  private String status = "initalized";
  private List<String> depotIdMaps = Collections.synchronizedList(new ArrayList<String>());
  private List<String> dscFiles = Collections.synchronizedList(new ArrayList<String>());

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public List<String> getDepotIdMapNames() {
    return new ArrayList<String>(depotIdMaps);
  }
  
  public void setDepotIdMapNames(List<String> depotIdMapNames) {
    this.depotIdMaps = depotIdMapNames;
  }
  
  public List<String> getDscFilenames() {
    return new ArrayList<String>(dscFiles);
  }
  
  public void setDscFilenames(List<String> dscFilenames) {
    this.dscFiles = dscFilenames;
  }

  public void addDepotIdMapNames(String depotIdMapName) {
    depotIdMaps.add(depotIdMapName);
  }

  public void addDscFilename(String dscFilename) {
    dscFiles.add(dscFilename);
  }
}
