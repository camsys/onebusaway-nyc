package org.onebusaway.nyc.admin.service.bundle.task;

public class HastusData {

  private String agencyId;
  private String gisDataDirectory;
  private String scheduleDataDirectory;
  
  public String getAgencyId() {
    return agencyId;
  }
  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }
  public String getGisDataDirectory() {
    return gisDataDirectory;
  }
  public void setGisDataDirectory(String gisDataDirectory) {
    this.gisDataDirectory = gisDataDirectory;
  }
  public String getScheduleDataDirectory() {
    return scheduleDataDirectory;
  }
  public void setScheduleDataDirectory(String scheduleDataDirectory) {
    this.scheduleDataDirectory = scheduleDataDirectory;
  }
  
  public String toString() {
    return "HastusData{[" + agencyId + "] gis=" + gisDataDirectory 
      + ", schedule=" + scheduleDataDirectory;
  }
  public boolean isValid() {
    return (gisDataDirectory != null && scheduleDataDirectory != null);
  }
}
