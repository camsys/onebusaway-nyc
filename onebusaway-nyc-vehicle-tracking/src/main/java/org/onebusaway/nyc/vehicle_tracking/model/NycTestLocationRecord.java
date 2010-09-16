package org.onebusaway.nyc.vehicle_tracking.model;

import java.io.InputStream;
import java.util.Date;

import org.onebusaway.gtfs.csv.schema.annotations.CsvField;
import org.onebusaway.gtfs.csv.schema.annotations.CsvFields;

@CsvFields(filename = "ivn-dsc.csv")
public class NycTestLocationRecord {

  @CsvField(name = "vid")
  private String vehicleId;

  private double lat;

  private double lon;

  @CsvField(name = "dt", mapping = DateTimeFieldMappingFactory.class)
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

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Date getTimestampAsDate() {
    return new Date(timestamp);
  }

  public void setDsc(String dsc) {
    this.dsc = dsc;
  }

  public String getDsc() {
    return dsc;
  }

  public static InputStream getTestData() {
    return NycTestLocationRecord.class.getResourceAsStream("ivn-dsc.csv");
  }
}
