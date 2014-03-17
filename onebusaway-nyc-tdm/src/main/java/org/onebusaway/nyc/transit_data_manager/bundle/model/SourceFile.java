package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.util.Date;

public class SourceFile extends BundleFile {

  private Date createdDate;
  private String uri;
  public Date getCreatedDate() {
    return createdDate;
  }
  
  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getUri() {
    return uri;
  }
  
  public void setUri(String relPathToBase) {
    this.uri = relPathToBase;
  }

}
