/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.presentation.model.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

public class LocationSearchResult implements SearchResult {

  private final NycGeocoderResult result;
  
  public LocationSearchResult(NycGeocoderResult result) {
    this.result = result;
  }
  
  @Override
  public String getType() {
    return "locationResult";    
  }

  @Override
  public String getName() {
    return getFormattedAddress();
  }
  
  public String getFormattedAddress() {
    return result.getFormattedAddress();
  }
  
  public String getNeighborhood() {
    return result.getNeighborhood();
  }

  public CoordinateBounds getBounds() {
    Double neLat = result.getNortheastLatitude();
    Double neLng = result.getNortheastLongitude();
    Double swLat = result.getSouthwestLatitude();
    Double swLng = result.getSouthwestLongitude();
    
    if(neLat != null && neLng != null && swLat != null && swLng != null) {
      return new CoordinateBounds(neLat, neLng, swLat, swLng);
    } else {
      return null;
    }
  }
  
  public boolean isRegion() {
    return result.isRegion();
  }
  
  public Double getLatitude() {
    return result.getLatitude();
  }
  
  public Double getLongitude() {
    return result.getLongitude();
  }  
}
