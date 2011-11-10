package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class Bundle {

  private String id;
  //private String name;
  //private List<String> applicableAgencyIds;
  private LocalDate serviceDateFrom;
  private LocalDate serviceDateTo;
  //private DateTime created;
  //private DateTime updated;
  private List<BundleFile> files;
  
  public String getId() {
    return id;
  }
//  public String getName() {
//    return name;
//  }
//  public List<String> getApplicableAgencyIds() {
//    return applicableAgencyIds;
//  }
  public LocalDate getServiceDateFrom() {
    return serviceDateFrom;
  }
  public LocalDate getServiceDateTo() {
    return serviceDateTo;
  }
//  public DateTime getCreated() {
//    return created;
//  }
//  public DateTime getUpdated() {
//    return updated;
//  }
  public List<BundleFile> getFiles() {
    return files;
  }
  public void setId(String id) {
    this.id = id;
  }
//  public void setName(String name) {
//    this.name = name;
//  }
//  public void setApplicableAgencyIds(List<String> applicableAgencyIds) {
//    this.applicableAgencyIds = applicableAgencyIds;
//  }
  public void setServiceDateFrom(LocalDate serviceDateFrom) {
    this.serviceDateFrom = serviceDateFrom;
  }
  public void setServiceDateTo(LocalDate serviceDateTo) {
    this.serviceDateTo = serviceDateTo;
  }
//  public void setCreated(DateTime created) {
//    this.created = created;
//  }
//  public void setUpdated(DateTime updated) {
//    this.updated = updated;
//  }
  public void setFiles(List<BundleFile> files) {
    this.files = files;
  }
  
  public boolean containsFile(String fileName) {
    boolean containsFile = false;
    
    for (BundleFile file : getFiles()) {
      if (file.getFilename().equals(fileName)){
        containsFile = true;
        break;
      }
    }
    
    return containsFile;
  }
}
