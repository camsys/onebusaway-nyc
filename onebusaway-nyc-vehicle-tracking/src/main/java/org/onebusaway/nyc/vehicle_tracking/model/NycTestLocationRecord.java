package org.onebusaway.nyc.vehicle_tracking.model;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import org.onebusaway.gtfs.csv.schema.annotations.CsvField;
import org.onebusaway.gtfs.csv.schema.annotations.CsvFields;

@CsvFields(filename = "ivn-dsc.csv")
public class NycTestLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @CsvField(name = "vid")
  private String vehicleId;

  private double lat;

  private double lon;

  @CsvField(name = "dt", mapping = DateTimeFieldMappingFactory.class)
  private long timestamp;

  @CsvField(optional = true)
  private String dsc;

  /****
   * Ground Truth Information
   ****/

  @CsvField(optional = true)
  private String actualBlockId;

  @CsvField(optional = true)
  private long actualServiceDate;

  @CsvField(optional = true)
  private double actualDistanceAlongBlock = Double.NaN;

  @CsvField(optional = true)
  private String actualDsc;

  @CsvField(optional = true)
  private double actualLat = Double.NaN;

  @CsvField(optional = true)
  private double actualLon = Double.NaN;

  @CsvField(optional = true)
  private String actualPhase = null;

  @CsvField(optional = true)
  private String actualStatus = null;

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

  /****
   * Ground Truth Data
   ****/

  public String getActualBlockId() {
    return actualBlockId;
  }

  public void setActualBlockId(String actualBlockId) {
    this.actualBlockId = actualBlockId;
  }

  public long getActualServiceDate() {
    return actualServiceDate;
  }

  public void setActualServiceDate(long actualServiceDate) {
    this.actualServiceDate = actualServiceDate;
  }

  public double getActualDistanceAlongBlock() {
    return actualDistanceAlongBlock;
  }

  public void setActualDistanceAlongBlock(double actualDistanceAlongBlock) {
    this.actualDistanceAlongBlock = actualDistanceAlongBlock;
  }

  public String getActualDsc() {
    return actualDsc;
  }

  public void setActualDsc(String actualDsc) {
    this.actualDsc = actualDsc;
  }

  public double getActualLat() {
    return actualLat;
  }

  public void setActualLat(double actualLat) {
    this.actualLat = actualLat;
  }

  public double getActualLon() {
    return actualLon;
  }

  public void setActualLon(double actualLon) {
    this.actualLon = actualLon;
  }

  public String getActualPhase() {
    return actualPhase;
  }

  public void setActualPhase(String actualPhase) {
    this.actualPhase = actualPhase;
  }

  public String getActualStatus() {
    return actualStatus;
  }

  public void setActualStatus(String actualStatus) {
    this.actualStatus = actualStatus;
  }

  public static InputStream getTestData() {
    return NycTestLocationRecord.class.getResourceAsStream("ivn-dsc.csv");
  }

  /**
   * Location data is considered missing if the values are NaN or if both are
   * zero
   */
  public boolean locationDataIsMissing() {
    return (Double.isNaN(this.lat) || Double.isNaN(this.lon))
        || (this.lat == 0.0 && this.lon == 0.0);
  }
}
