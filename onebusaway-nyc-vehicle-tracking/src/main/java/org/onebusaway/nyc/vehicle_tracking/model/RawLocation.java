package org.onebusaway.nyc.vehicle_tracking.model;

import org.hibernate.annotations.AccessType;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oba_raw_location")
@AccessType("field")
public class RawLocation {
  
  @Id
  private long id;
  
  private long time;
  
  private double Latitude;
  private double Longitude;
  private double Bearing;

  public void setId(long id) {
    this.id = id;
  }
  public long getId() {
    return id;
  }
  public void setTime(long time) {
    this.time = time;
  }
  public long getTime() {
    return time;
  }
  public void setLatitude(double latitude) {
    Latitude = latitude;
  }
  public double getLatitude() {
    return Latitude;
  }
  public void setLongitude(double longitude) {
    Longitude = longitude;
  }
  public double getLongitude() {
    return Longitude;
  }
  public void setBearing(double bearing) {
    Bearing = bearing;
  }
  public double getBearing() {
    return Bearing;
  }

}
