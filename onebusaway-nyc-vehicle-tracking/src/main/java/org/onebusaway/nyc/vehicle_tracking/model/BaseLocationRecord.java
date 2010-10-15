package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.geospatial.model.CoordinateBounds;

public class BaseLocationRecord {

  private String baseName;

  private double latFrom;

  private double lonFrom;

  private double latTo;

  private double lonTo;

  public String getBaseName() {
    return baseName;
  }

  public void setBaseName(String baseName) {
    this.baseName = baseName;
  }

  public double getLatFrom() {
    return latFrom;
  }

  public void setLatFrom(double latFrom) {
    this.latFrom = latFrom;
  }

  public double getLonFrom() {
    return lonFrom;
  }

  public void setLonFrom(double lonFrom) {
    this.lonFrom = lonFrom;
  }

  public double getLatTo() {
    return latTo;
  }

  public void setLatTo(double latTo) {
    this.latTo = latTo;
  }

  public double getLonTo() {
    return lonTo;
  }

  public void setLonTo(double lonTo) {
    this.lonTo = lonTo;
  }
  
  public CoordinateBounds toBounds() {
    return new CoordinateBounds(latFrom,lonFrom,latTo,lonTo);
  }
}
