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
package org.onebusaway.nyc.geocoder.impl;

import org.onebusaway.geocoder.impl.GoogleAddressComponent;
import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.model.GeocoderResults;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;

import org.apache.commons.digester.Digester;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GoogleGeocoderImpl implements NycGeocoderService, GeocoderService {

  private static final String BASE_URL = "http://maps.google.com/maps/api/geocode/xml";
  
  private boolean _sensor = false;

  private CoordinateBounds _resultBiasingBounds = null;

  private String _usStateFilter = null;
  
  public void setSensor(boolean sensor) {
    _sensor = sensor;
  }

  public void setResultBiasingBounds(CoordinateBounds bounds) {
    _resultBiasingBounds = bounds;
  }
  
  public void setUsStateFilter(String state) {
    _usStateFilter = state;
  }

  // (method to make legacy OBA components that use the geocoder happy...)
  public GeocoderResults geocode(String location) {
    GeocoderResults output = new GeocoderResults();
    for(NycGeocoderResult r : nycGeocode(location)) {
      output.addResult((GeocoderResult)r);
    }
    return output;
  }
  
  public List<NycGeocoderResult> nycGeocode(String location) {
    StringBuilder b = new StringBuilder();
    b.append(BASE_URL);
    b.append("?");

    b.append("sensor=").append(_sensor);
    
    if(_resultBiasingBounds != null) {
      b.append("&bounds=").append(_resultBiasingBounds.getMinLat() + "," + _resultBiasingBounds.getMinLon() 
          + "|" + _resultBiasingBounds.getMaxLat() + "," + _resultBiasingBounds.getMaxLon());
    }
    
    try {
      String encodedLocation = URLEncoder.encode(location, "UTF-8");
      b.append("&address=").append(encodedLocation);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("unknown encoding: UTF-8");
    }

    URL url = null;
    try {
      url = new URL(b.toString());
    } catch(MalformedURLException e) {
      throw new IllegalStateException(e.getMessage());
    }

    List<NycGeocoderResult> results = new ArrayList<NycGeocoderResult>();
    Digester digester = createDigester();
    digester.push(results);

    InputStream inputStream = null;
    try {
      inputStream = url.openStream();
      digester.parse(inputStream);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    } finally {
      if (inputStream != null) {
        try { inputStream.close(); } catch (Exception ex) {}
      }
    }
    
    results = filterResults(results);
    
    return results;
  }

  private List<NycGeocoderResult> filterResults(List<NycGeocoderResult> input) {
    if(_usStateFilter == null)
      return input;
    
    List<NycGeocoderResult> output = new ArrayList<NycGeocoderResult>();
    for(NycGeocoderResult result : input) {
      if(result.getAdministrativeArea() != null && result.getAdministrativeArea().equals(_usStateFilter))
        output.add(result);
    }
    return output;
  }
  
  private Digester createDigester() {
    Digester digester = new Digester();

    digester.addObjectCreate("GeocodeResponse/result", NycGeocoderResult.class);
    digester.addObjectCreate("GeocodeResponse/result/address_component", GoogleAddressComponent.class);

    digester.addCallMethod("GeocodeResponse/result/address_component/long_name", "setLongName", 0);
    digester.addCallMethod("GeocodeResponse/result/address_component/short_name", "setShortName", 0);
    digester.addCallMethod("GeocodeResponse/result/address_component/type", "addType", 0);
    digester.addSetNext("GeocodeResponse/result/address_component", "addAddressComponent");
    
    Class<?>[] dType = {Double.class};

    digester.addCallMethod("GeocodeResponse/result/formatted_address", "setFormattedAddress", 0);
    digester.addCallMethod("GeocodeResponse/result/geometry/location/lat", "setLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/location/lng", "setLongitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/southwest/lat", "setSouthwestLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/southwest/lng", "setSouthwestLongitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/northeast/lat", "setNortheastLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/northeast/lng", "setNortheastLongitude", 0, dType);
    digester.addSetNext("GeocodeResponse/result", "add");

    return digester;
  }
}
