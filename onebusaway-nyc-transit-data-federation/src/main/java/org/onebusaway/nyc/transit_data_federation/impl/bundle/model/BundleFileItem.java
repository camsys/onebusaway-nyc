package org.onebusaway.nyc.transit_data_federation.impl.bundle.model;

import java.io.Serializable;

public class BundleFileItem implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private String filename;
  
  private String md5;
  
  public BundleFileItem() {}

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

}
