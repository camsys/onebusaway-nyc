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

import org.onebusaway.nyc.geocoder.model.NycCustomGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * A geocoder that works against NYC Geocoder API Adapter REST-ful API. 
 * @author lcaraballo
 *
 */
public class NycAutocompleteAdapterImpl extends FilteredGeocoderBase {

  private static Logger _log = LoggerFactory.getLogger(NycAutocompleteAdapterImpl.class);

  private static final String GEOCODE_URL_PREFIX = "http://localhost:8180/autocomplete";
  
  private static final Gson gson = new Gson();
  
  public NycAutocompleteAdapterImpl(){}
  public NycAutocompleteAdapterImpl(FilteredGeocoderBase template){
	  super(template);
  }
  
  public List<NycGeocoderResult> nycGeocode(String location) {
    try {
      List<NycGeocoderResult> results = new ArrayList<NycGeocoderResult>();

      StringBuilder q = new StringBuilder();
    
      String encodedLocation = URLEncoder.encode(location, "UTF-8");
      q.append("address=").append(encodedLocation);
    
      URL url = new URL(GEOCODE_URL_PREFIX + "?" + q.toString());         

      _log.debug("Requesting " + url.toString());
      InputStream inputStream = url.openStream();
      
      results = getResults(inputStream);
      _log.debug("Got " + results.size() + " geocoder results.");

      results = filterResultsByWktPolygon(results);
      _log.debug("Have " + results.size() + " geocoder results AFTER filtering.");

      return results;
    } catch (Exception e) {
      _log.error("Geocoding error: " + e.getMessage());
      return null;
    }
  }
  
  public List<NycGeocoderResult> getResults(InputStream input){
	  List<NycGeocoderResult> results = new ArrayList<NycGeocoderResult>();
	  results= gson.fromJson(new InputStreamReader(input),  new TypeToken<List<NycCustomGeocoderResult>>(){}.getType());
	  return results;
  }
  
}
