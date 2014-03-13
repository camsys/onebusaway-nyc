package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.util.Date;

public class BundleMetadata {

  private String id;
  private String name;
  private Date serviceDateFrom;
  private Date serviceDataTo;
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public Date getServiceDateFrom() {
    return serviceDateFrom;
  }
  public void setServiceDateFrom(Date serviceDateFrom) {
    this.serviceDateFrom = serviceDateFrom;
  }
  public Date getServiceDataTo() {
    return serviceDataTo;
  }
  public void setServiceDataTo(Date serviceDataTo) {
    this.serviceDataTo = serviceDataTo;
  }
  
}
