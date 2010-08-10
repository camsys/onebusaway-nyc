package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.onebusaway.gtfs.csv.schema.annotations.CsvFields;

@CsvFields(filename = "ivn-dsc.csv", fieldOrder = {
    "vehicleId", "date", "time", "lat", "lon", "timestamp", "dsc", "new_dsc"})
public class NycTestLocationRecord {
  private String vehicleId;

  private double lat;
  private double lon;
  private long timestamp;
  private String dsc;

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLat() {
    return lat;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public double getLon() {
    return lon;
  }

  public void setTimestamp(String timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    try {
      this.timestamp = sdf.parse(timestamp).getTime();
    } catch (ParseException e) {
      throw new RuntimeException("error parsing datetime " + timestamp, e);
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setDsc(String dsc) {
    this.dsc = dsc;
  }

  public String getDsc() {
    return dsc;
  }
}
