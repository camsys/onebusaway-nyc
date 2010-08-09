package org.onebusaway.nyc.vehicle_tracking.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;

@Entity
@Table(name = "oba_nyc_raw_location")
@AccessType("field")
public class NycVehicleLocationRecord {

  @Id
  private long id;

  private long time;

  private double latitude;

  private double longitude;

  private double bearing;

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
    this.latitude = latitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setBearing(double bearing) {
    this.bearing = bearing;
  }

  public double getBearing() {
    return bearing;
  }

}
