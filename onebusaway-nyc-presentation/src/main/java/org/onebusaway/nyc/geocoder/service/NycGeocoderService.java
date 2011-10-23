package org.onebusaway.nyc.geocoder.service;

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;

import java.util.List;

public interface NycGeocoderService {

  /*
   * We have to rename this method to nycGeocode because naming it geocode
   * conflicts with the GeocoderService interface we must implement to make
   * other OBA components happy. The latter service uses a typed list bean, which
   * we can't change to use NycGeocoderResults.
   */
  public List<NycGeocoderResult> nycGeocode(String location);

}
