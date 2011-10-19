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
package org.onebusaway.nyc.presentation.impl;

import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.impl.search.RouteSearchServiceImpl;
import org.onebusaway.nyc.presentation.impl.search.StopSearchServiceImpl;
import org.onebusaway.nyc.presentation.impl.sort.SearchResultComparator;
import org.onebusaway.nyc.presentation.model.EnumDisplayMedia;
import org.onebusaway.nyc.presentation.model.search.LocationSearchResult;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.service.NycSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NycSearchServiceImpl implements NycSearchService {
  
  private final static Pattern routePattern = 
      Pattern.compile("(?:[BMQSX]|BX)[0-9]+", Pattern.CASE_INSENSITIVE);

  @Autowired
  private StopSearchServiceImpl _stopSearchService;
  
  @Autowired
  private RouteSearchServiceImpl _routeSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;
  
  @Override
  public void setTime(Date time) {
    _stopSearchService.setTime(time);
    _routeSearchService.setTime(time);
  }
 
  @Override
  public List<SearchResult> search(String q, EnumDisplayMedia media) {
    List<SearchResult> results = new ArrayList<SearchResult>();

    if (q == null)
      return results;

    q = q.trim();
    if (q.isEmpty())
      return results;

    String[] fields = q.split(" ");

    // we are probably a query for a route or a stop ID
    if (fields.length == 1) {
      if(isStop(q)) {
        // one stop
        results.add(_stopSearchService.makeResultFor(q, media));
      } else if(isRoute(q)) {
        // one route
        results.addAll(_routeSearchService.makeResultFor(q, media));
      } else {
        // ??
        results.addAll(generateResultsFromGeocode(q, media));        
      }
    } else {
      // intersection, zipcode, neighborhood, etc.
      results.addAll(generateResultsFromGeocode(q, media));        
    }

    Collections.sort(results, new SearchResultComparator());

    return results;
  }

  @Override
  public boolean isStop(String s) {
    // stops are 6 digits
    int n = s.length();
    if (n != 6)
      return false;
    for (int i = 0; i < n; i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isRoute(String s) {
    Matcher matcher = routePattern.matcher(s);
    return matcher.matches();
  }
  
  private List<SearchResult> generateResultsFromGeocode(String q, EnumDisplayMedia media) {
    List<SearchResult> results = new ArrayList<SearchResult>();

    // are any of the tokens a route filter?
    String routeToFilterBy = null;
    for(String token : q.split(" ")) {
      if(isRoute(token)) {
        routeToFilterBy = token;
        break;
      }
    }
    
    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(q);

    if(media == EnumDisplayMedia.DESKTOP_WEB) {
      // return locations to front-end
      for(NycGeocoderResult result : geocoderResults) {
        results.add(new LocationSearchResult(result));
      }
    
    } else if(media == EnumDisplayMedia.MOBILE_WEB || media == EnumDisplayMedia.SMS) {
      if(geocoderResults.size() > 1) {
        // if there is more than one option, present the user with possible unambiguous
        // things to choose from
        for(NycGeocoderResult result : geocoderResults) {
           results.add(new LocationSearchResult(result));
        }
        
        return results;

      // return stops near the place entered
      } else if(geocoderResults.size() == 1) {
        GeocoderResult theResult = geocoderResults.get(0);

        results.addAll(_stopSearchService.resultsForLocation(theResult.getLatitude(), 
              theResult.getLongitude(), media));
      }
    }
    
    // filter results by route, if needed
    if(routeToFilterBy != null) {
      results = filterByRoute(results, routeToFilterBy);
    }
    
    return results;
  }
  
  private List<SearchResult> filterByRoute(List<SearchResult> results, String routeFilter) {
    List<SearchResult> output = new ArrayList<SearchResult>();

    for(SearchResult result : results) {
      if(result.getType() == "route") {
        RouteSearchResult routeResult = (RouteSearchResult)result;
        if(routeResult.getName().equals(routeFilter))
          output.add(routeResult);
      } else {
        output.add(result);
      }
    }
    
    return output;
  }
}
