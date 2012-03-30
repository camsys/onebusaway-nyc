package org.onebusaway.nyc.geocoder.service;

import org.onebusaway.geospatial.model.CoordinateBounds;

public interface NycGeocoderResult {

  public Double getLatitude();

  public Double getLongitude();

  public String getNeighborhood();

  public String getFormattedAddress();

  public CoordinateBounds getBounds();

  public boolean isRegion();

}