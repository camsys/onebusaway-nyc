package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;

import java.util.List;

/**
 * Location search result.
 * @author jmaki
 *
 */
public class GeocodeResult implements SearchResult {

  private NycGeocoderResult result;
  
  private List<SearchResult> nearbyRoutes;
  
  public GeocodeResult(NycGeocoderResult result, List<SearchResult> nearbyRoutes) {
    this.result = result;
    this.nearbyRoutes = nearbyRoutes;
  }
  
  public String getFormattedAddress() {
    return result.getFormattedAddress();
  }
  
  public String getNeighborhood() {
    return result.getNeighborhood();
  }
  
  public Double getLatitude() {
    return result.getLatitude();
  }

  public Double getLongitude() {
    return result.getLongitude();
  }  
  
  public Boolean getIsRegion() {
    return result.isRegion();
  }
  
  public CoordinateBounds getBounds() {
    return result.getBounds();
  }
  
  public List<SearchResult> getNearbyRoutes() {
    return nearbyRoutes;
  }
}
