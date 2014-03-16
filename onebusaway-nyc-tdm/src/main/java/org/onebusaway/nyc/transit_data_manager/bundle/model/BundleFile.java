package org.onebusaway.nyc.transit_data_manager.bundle.model;

public class BundleFile {

  private String filename;
  private String md5;

  public String getFilename() {
    return filename;
  }

  public String getMd5() {
    return md5;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }
}
