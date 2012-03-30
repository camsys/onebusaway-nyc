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

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.model.BingGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BingGeocoderImpl extends FilteredGeocoderBase {

  private static Logger _log = LoggerFactory.getLogger(BingGeocoderImpl.class);

  private static final String GEOCODE_URL_PREFIX = "http://dev.virtualearth.net/REST/v1/Locations";
  
  @Autowired
  private ConfigurationService _configurationService;

  private CoordinateBounds _resultBiasingBounds = null;
    
  public void setResultBiasingBounds(CoordinateBounds bounds) {
    _resultBiasingBounds = bounds;
  }

  public List<NycGeocoderResult> nycGeocode(String location) {
    try {
      List<NycGeocoderResult> results = new ArrayList<NycGeocoderResult>();

      StringBuilder q = new StringBuilder();
      q.append("includeNeighborhood=true");
      q.append("&output=xml");
    
      String encodedLocation = URLEncoder.encode(location, "UTF-8");
      q.append("&query=").append(encodedLocation);
    
      if(_resultBiasingBounds != null) {
        q.append("&userMapView=").append(
            _resultBiasingBounds.getMinLat() + "," + 
            _resultBiasingBounds.getMinLon() + "," + 
            _resultBiasingBounds.getMaxLat() + "," + 
            _resultBiasingBounds.getMaxLon());
      }

      String secretKey = 
          _configurationService.getConfigurationValueAsString("display.bingMapsKey", null);    
      
      if(secretKey != null && !StringUtils.isEmpty(secretKey)) {
        q.append("&key=").append(secretKey);
      }
    
      URL url = new URL(GEOCODE_URL_PREFIX + "?" + q.toString());        
      
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

    digester.addObjectCreate("Response/ResourceSets/ResourceSet/Resources/Location", BingGeocoderResult.class);    
    
    Class<?>[] dType = {Double.class};
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/Address/FormattedAddress", "setFormattedAddress", 0);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/Address/Neighborhood", "setNeighborhood", 0);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/Address/Locality", "setLocality", 0);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/Point/Latitude", "setLatitude", 0, dType);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/Point/Longitude", "setLongitude", 0, dType);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/BoundingBox/SouthLatitude", "setSouthwestLatitude", 0, dType);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/BoundingBox/WestLongitude", "setSouthwestLongitude", 0, dType);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/BoundingBox/NorthLatitude", "setNortheastLatitude", 0, dType);
    digester.addCallMethod("Response/ResourceSets/ResourceSet/Resources/Location/BoundingBox/EastLongitude", "setNortheastLongitude", 0, dType);

    digester.addSetNext("Response/ResourceSets/ResourceSet/Resources/Location", "add");

    return digester;
  }
}
