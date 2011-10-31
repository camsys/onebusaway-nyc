package org.onebusaway.nyc.transit_data_manager.barcode.model;

public class MtaBarcode {

  private int stopId;
  private String contents;
  
  public int getStopId() {
    return stopId;
  }
  public String getContents() {
    return contents;
  }
  public void setStopId(int stopId) {
    this.stopId = stopId;
  }
  public void setContents(String contents) {
    this.contents = contents;
  }
  
  
}
