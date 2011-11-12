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
package org.onebusaway.nyc.geocoder.model;

import org.onebusaway.geocoder.impl.GoogleAddressComponent;
import org.onebusaway.geocoder.impl.GoogleGeocoderResult;
import org.onebusaway.geospatial.model.CoordinateBounds;

public class NycGeocoderResult extends GoogleGeocoderResult {

  private static final long serialVersionUID = 1L;
  
  private String formattedAddress;

  private String neighborhood;

  private Double northeastLatitude = null;

  private Double northeastLongitude = null;

  private Double southwestLatitude = null;

  private Double southwestLongitude = null;

  // address
  public void setFormattedAddress(String a) {
    formattedAddress = a;
  }
  
  public String getFormattedAddress() {
    if(formattedAddress != null)
      formattedAddress = formattedAddress.replace(", USA", "");
    return formattedAddress;
  }

  // neighborhood
  @Override
  public void addAddressComponent(GoogleAddressComponent addressComponent) {
    super.addAddressComponent(addressComponent);
    
    if(addressComponent.getTypes().contains("neighborhood"))
      this.neighborhood = addressComponent.getLongName();
  }
  
  public String getNeighborhood() {
    return neighborhood;
  }

  // bounds
  public Double getNortheastLatitude() {
    return northeastLatitude;
  }

  public void setNortheastLatitude(Double northeastLatitude) {
    this.northeastLatitude = northeastLatitude;
  }

  public Double getNortheastLongitude() {
    return northeastLongitude;
  }

  public void setNortheastLongitude(Double northeastLongitude) {
    this.northeastLongitude = northeastLongitude;
  }

  public Double getSouthwestLatitude() {
    return southwestLatitude;
  }

  public void setSouthwestLatitude(Double southwestLatitude) {
    this.southwestLatitude = southwestLatitude;
  }

  public Double getSouthwestLongitude() {
    return southwestLongitude;
  }

  public void setSouthwestLongitude(Double southwestLongitude) {
    this.southwestLongitude = southwestLongitude;
  }  

  public CoordinateBounds getBounds() {
    if(northeastLatitude != null && northeastLongitude != null 
        && southwestLatitude != null && southwestLongitude != null) {
      
      return new CoordinateBounds(northeastLatitude, northeastLongitude, 
          southwestLatitude, southwestLongitude);
    } else {
      return null;
    }    
  }
  
  public boolean isRegion() {
    return getBounds() != null;
  }
}
