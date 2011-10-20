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

import org.onebusaway.geocoder.impl.GoogleGeocoderResult;

public class NycGeocoderResult extends GoogleGeocoderResult {

  private static final long serialVersionUID = 1L;
  
  private String formattedAddress;
    
  private Double northEastLatitude = null;

  private Double northEastLongitude = null;

  private Double southWestLatitude = null;

  private Double southWestLongitude = null;

  public void setFormattedAddress(String a) {
    formattedAddress = a;
  }
  
  @Override
  public String getAddress() {
    return formattedAddress;
  }

  public Double getNorthEastLatitude() {
    return northEastLatitude;
  }

  public void setNorthEastLatitude(Double northEastLatitude) {
    this.northEastLatitude = northEastLatitude;
  }

  public Double getNorthEastLongitude() {
    return northEastLongitude;
  }

  public void setNorthEastLongitude(Double northEastLongitude) {
    this.northEastLongitude = northEastLongitude;
  }

  public Double getSouthWestLatitude() {
    return southWestLatitude;
  }

  public void setSouthWestLatitude(Double southWestLatitude) {
    this.southWestLatitude = southWestLatitude;
  }

  public Double getSouthWestLongitude() {
    return southWestLongitude;
  }

  public void setSouthWestLongitude(Double southWestLongitude) {
    this.southWestLongitude = southWestLongitude;
  }  

}
