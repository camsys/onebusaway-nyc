package org.onebusaway.nyc.transit_data_federation.model.bundle;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class BundleItem implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  
  private Date serviceDateFrom;
  
  private Date serviceDateTo;
  
  private ArrayList<BundleFileItem> files;
 
  public BundleItem() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getServiceDateFrom() {
    return serviceDateFrom;
  }

  public void setServiceDateFrom(Date serviceDateFrom) {
    this.serviceDateFrom = serviceDateFrom;
  }

  public Date getServiceDateTo() {
    return serviceDateTo;
  }

  public void setServiceDateTo(Date serviceDateTo) {
    this.serviceDateTo = serviceDateTo;
  }

  public ArrayList<BundleFileItem> getFiles() {
    return files;
  }

  public void setFiles(ArrayList<BundleFileItem> files) {
    this.files = files;
  }

}
