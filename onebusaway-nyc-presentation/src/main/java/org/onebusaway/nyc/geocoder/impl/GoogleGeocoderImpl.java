/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.geocoder.impl;

import org.onebusaway.geocoder.impl.GoogleAddressComponent;
import org.onebusaway.nyc.geocoder.model.GoogleGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A geocoder that queries against Google's REST-ful Enterprise API. 
 * @author jmaki
 *
 */
public class GoogleGeocoderImpl extends FilteredGeocoderBase {

  private static Logger _log = LoggerFactory.getLogger(GoogleGeocoderImpl.class);

  private static final String GEOCODE_URL_PREFIX = "https://maps.googleapis.com";
  
  private static final String GEOCODE_PATH = "/maps/api/geocode/xml";

  @Autowired  
  private boolean _sensor = false;
  
  public GoogleGeocoderImpl(){}
  public GoogleGeocoderImpl(FilteredGeocoderBase template){
	  super(template);
  }
  
  public void setSensor(boolean sensor) {
    _sensor = sensor;
  }
  
  public List<NycGeocoderResult> nycGeocode(String location) {
    try {
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

      String apiKey =
          _configurationService.getConfigurationValueAsString("display.googleMapsGeocoderApiKey", null);

      if (StringUtils.isEmpty(apiKey)) {
        _log.warn("No googleMapsApiKey configured. Not accessing Google.");
        return Collections.emptyList();
      }

      q.append("&key=").append(apiKey);

      URL url = new URL(GEOCODE_URL_PREFIX + GEOCODE_PATH + "?" + q.toString());
      
      Digester digester = createDigester();
      digester.push(results);

      _log.debug("Requesting " + url.toString());
      InputStream inputStream = url.openStream();

      digester.parse(inputStream);    
      _log.debug("Got " + results.size() + " geocoder results.");

      results = filterResultsByWktPolygon(results);
      _log.debug("Have " + results.size() + " geocoder results AFTER filtering.");

      return results;
    } catch (Exception e) {
      _log.error("Geocoding error: " + e.getMessage());
      return null;
    }
  }
  
  private Digester createDigester() {
    Digester digester = new Digester();

    digester.addObjectCreate("GeocodeResponse/result", GoogleGeocoderResult.class);

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
