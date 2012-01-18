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
import org.onebusaway.geocoder.model.GeocoderResults;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.digester.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleGeocoderImpl implements NycGeocoderService, GeocoderService {

  private static Logger _log = LoggerFactory.getLogger(GoogleGeocoderImpl.class);

  private static final String GEOCODE_URL_PREFIX = "http://maps.googleapis.com";
  
  private static final String GEOCODE_PATH = "/maps/api/geocode/xml";

  @Autowired
  private ConfigurationService _configurationService;

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
    return null;
  }
  
  public List<NycGeocoderResult> nycGeocode(String location) {
    try {
      String clientId = 
          _configurationService.getConfigurationValueAsString("display.googleMapsClientId", null);    

      String secretKey = 
          _configurationService.getConfigurationValueAsString("display.googleMapsSecretKey", null);    
      
      List<NycGeocoderResult> results = new ArrayList<NycGeocoderResult>();

      StringBuilder q = new StringBuilder();
      q.append("sensor=").append(_sensor);
    
      String encodedLocation = URLEncoder.encode(location, "UTF-8");
      q.append("&address=").append(encodedLocation);
    
      if(_resultBiasingBounds != null) {
        q.append("&bounds=").append(
            _resultBiasingBounds.getMinLat() + "," + 
            _resultBiasingBounds.getMinLon() + "|" + 
            _resultBiasingBounds.getMaxLat() + "," + 
            _resultBiasingBounds.getMaxLon());
      }
    
      if(clientId != null && secretKey != null && !StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(secretKey)) {
        q.append("&client=").append(clientId);
      }
    
      URL url = null;
      if(secretKey != null && clientId != null && !StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(secretKey)) {
        GoogleUrlAuthentication urlSigner = new GoogleUrlAuthentication(secretKey);

        url = new URL(GEOCODE_URL_PREFIX + urlSigner.signRequest(GEOCODE_PATH + "?" + q.toString()));
      } else {
        url = new URL(GEOCODE_URL_PREFIX + GEOCODE_PATH + "?" + q.toString());        
      }
      
      Digester digester = createDigester();
      digester.push(results);

      _log.info("Requesting " + url.toString());
      InputStream inputStream = url.openStream();
      digester.parse(inputStream);    
      _log.info("Got " + results.size() + " geocoder results.");

      results = filterResults(results);
      _log.info("Have " + results.size() + " geocoder results AFTER filtering.");

      results = deDuplicateResults(results);
      _log.info("Have " + results.size() + " geocoder results AFTER deduplicating.");

      return results;
    } catch (Exception e) {
      _log.error("Geocoding error: " + e.getMessage());
      return null;
    }
  }

  // FIXME: google's returning two "Staten Island, NY, USA" results for the 
  // search "Staten Island". filter them!
  private List<NycGeocoderResult> deDuplicateResults(List<NycGeocoderResult> input) {
    if(input == null || (input != null && input.size() == 1)) {
      return input;
    }
    
    Map<String, NycGeocoderResult> output = new HashMap<String, NycGeocoderResult>();
    for(NycGeocoderResult result : input) {
      output.put(result.getAddress(), result);
    }

    return new ArrayList<NycGeocoderResult>(output.values());
  }
  
  private List<NycGeocoderResult> filterResults(List<NycGeocoderResult> input) {
    if(_usStateFilter == null) {
      return input;
    }
    
    List<NycGeocoderResult> output = new ArrayList<NycGeocoderResult>();
    for(NycGeocoderResult result : input) {
      if(result.getAdministrativeArea() != null && result.getAdministrativeArea().equals(_usStateFilter)) {
        output.add(result);
      }
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
