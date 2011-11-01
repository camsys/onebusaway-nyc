package org.onebusaway.nyc.transit_data_manager.barcode.model;

public class MtaBarcode {

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MtaBarcode other = (MtaBarcode) obj;

    if ((other.getStopId() != -1 && getStopId() != -1) && (other.getStopId() == getStopId())) {
      return true;
    } else if ((other.getStopIdStr() != null && getStopIdStr() != null) && (other.getStopIdStr().equalsIgnoreCase(getStopIdStr()))) {
      return true;
    } 

    return false;
  }
  
  private int stopId = -1;
  private String stopIdStr;
  private String contents;
  
  public int getStopId() {
    return stopId;
  }
  public String getStopIdStr() {
    return stopIdStr;
  }
  public String getContents() {
    return contents;
  }
  public void setStopId(int stopId) {
    this.stopId = stopId;
  }
  public void setStopIdStr(String stopIdStr) {
    this.stopIdStr = stopIdStr;
  }
  public void setContents(String contents) {
    this.contents = contents;
  }
  
  
  
}
