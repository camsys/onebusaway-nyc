package org.onebusaway.nyc.transit_data_manager.barcode.model;

public class MtaBarcode {

  public MtaBarcode (String contents) {
    this.contents = contents;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MtaBarcode other = (MtaBarcode) obj;

    if ((other.getContents() != null && getContents() != null) && (other.getContents().equals(getContents()))) {
      return true;
    } else {
      return false;
    }
    
  }
  
  @Override
  public int hashCode () {
    return contents.hashCode();
  }
  
  private String contents = null;
  private String stopIdStr = null;
  
  
  public String getStopIdStr() {
    return stopIdStr;
  }

  public void setStopIdStr(String stopIdStr) {
    this.stopIdStr = stopIdStr;
  }

  public String getContents() {
    return contents;
  }
  
  
}
