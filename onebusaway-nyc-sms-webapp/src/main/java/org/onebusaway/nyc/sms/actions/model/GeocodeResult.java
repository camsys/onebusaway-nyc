package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;

/**
 * Ambiguous address top-level search result.
 * @author jmaki
 *
 */
public class GeocodeResult implements SearchResult {

  private NycGeocoderResult result;
  
  public GeocodeResult(NycGeocoderResult result) {
    this.result = result;
  }

  public String getFormattedAddress() {
    return result.getFormattedAddress();
  }
  
  public Double getLatitude() {
    return result.getLatitude();
  }
  
  public Double getLongitude() {
    return result.getLongitude();
  }
  
  public String getNeighborhood() {
    return result.getNeighborhood();
  }
  
  public Boolean isRegion() {
    return result.isRegion();
  }
  
}
