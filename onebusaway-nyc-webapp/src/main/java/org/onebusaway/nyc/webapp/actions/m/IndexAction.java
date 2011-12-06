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
package org.onebusaway.nyc.webapp.actions.m;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.impl.sort.LocationSensitiveSearchResultComparator;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.SearchResultCollection;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebLocationResult;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebPresentationModelFactory;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private static final String CURRENT_LOCATION_TEXT = "(Current Location)";
  
  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private RouteSearchService _routeSearchService;

  @Autowired
  private StopSearchService _stopSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;

  @Autowired
  private ConfigurationService _configurationService;

  private MobileWebPresentationModelFactory _factory = null;
  
  private SearchResultCollection _searchResults = new SearchResultCollection();
  
  private String _q;
  
  private CoordinatePoint _location;
  
  public void setQ(String q) {
    if(q != null)
      this._q = q.trim();
    else
      this._q = null;
  }

  public void setL(String location) {
    String[] locationParts = location.split(",");
    if(locationParts.length == 2) {
      this._location = new CoordinatePoint(Double.parseDouble(locationParts[0]), 
          Double.parseDouble(locationParts[1]));
    } else
      this._location = null;
  }

  public String execute() throws Exception {
    if(_q == null)
      return SUCCESS;

    _factory = new MobileWebPresentationModelFactory(_realtimeService, _configurationService);
    _stopSearchService.setModelFactory(_factory);
    _routeSearchService.setModelFactory(_factory);
    
    // empty query with location means search for current location
    if(_location != null && (_q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT)))) {
      _searchResults.addAll(_stopSearchService.resultsForLocation(_location.getLat(), _location.getLon()));        

    } else {
      if(_q.isEmpty())
        return SUCCESS;
      
      // try as stop ID
      _searchResults.addAll(_stopSearchService.resultsForQuery(_q));

      // try as route ID
      if(_searchResults.size() == 0)
        _searchResults.addAll(_routeSearchService.resultsForQuery(_q));

      // nothing? geocode it!
      if(_searchResults.size() == 0)
        _searchResults.addAll(generateResultsFromGeocode(_q));        
    }

    // apply route filter
    _searchResults = filterResults(_searchResults, _q);
    
    Collections.sort(_searchResults, new LocationSensitiveSearchResultComparator(_location));

    return SUCCESS;
  }
  
  /**
   * PRIVATE HELPER METHODS
   */
  private SearchResultCollection filterResults(SearchResultCollection results, String q) {    
    List<String> routesToFilterBy = new ArrayList<String>();
    for(String token : q.split(" ")) {
      if(_routeSearchService.isRoute(token)) {
        routesToFilterBy.add(token);
      }
    }

    if(routesToFilterBy.size() == 0) {
      return results;
    }

    SearchResultCollection newResults = new SearchResultCollection();
    for(SearchResult _result : results) {
      if(_result instanceof MobileWebLocationResult) {
        MobileWebLocationResult result = (MobileWebLocationResult)_result;
        
        for(RouteResult nearbyRoute : result.getNearbyRoutes()) {
          if(routesToFilterBy.contains(nearbyRoute.getRouteIdWithoutAgency())) {
            newResults.add(result);
          }
        }
        
      } else if (_result instanceof StopResult) {
        StopResult result = (StopResult)_result;
        
        List<RouteResult> newRoutesAvailable = new ArrayList<RouteResult>();
        for(RouteResult routeAvailable : result.getRoutesAvailable()) {
          if(routesToFilterBy.contains(routeAvailable.getRouteIdWithoutAgency())) {
            newRoutesAvailable.add(routeAvailable);
          }
        }
        
        result.getRoutesAvailable().clear();
        result.getRoutesAvailable().addAll(newRoutesAvailable);
        
        newResults.add(result);

      // pass through
      } else {
        newResults.add(_result);
      }
    }
       
    return newResults;
  }
  
  private List<SearchResult> generateResultsFromGeocode(String q) {
    List<SearchResult> results = new ArrayList<SearchResult>();
    
    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(q);

    // one location: if region, show routes, if point, show stops nearby
    if(geocoderResults.size() == 1) {
      NycGeocoderResult result = geocoderResults.get(0);

      if(result.isRegion()) {
        results.addAll(_routeSearchService.resultsForLocation(result.getBounds()));
      } else {
        results.addAll(_stopSearchService.resultsForLocation(result.getLatitude(), result.getLongitude()));
      }
      
      return results;

    // location disambiguation w/ nearby routes
    } else {
      for(NycGeocoderResult result : geocoderResults) {
        MobileWebLocationResult locationSearchResult = new MobileWebLocationResult(result);

        if(result.isRegion()) {
          locationSearchResult.setNearbyRoutes(_routeSearchService.resultsForLocation(result.getBounds()));
        } else {
          locationSearchResult.setNearbyRoutes(_routeSearchService.resultsForLocation(result.getLatitude(), 
              result.getLongitude()));
        }
        
        results.add(locationSearchResult);
      }
    }

    return results;
  }  
  
  /**
   * METHODS FOR VIEWS
   */
  // Adapted from http://code.google.com/mobile/analytics/docs/web/#jsp
  public String getGoogleAnalyticsTrackingUrl() {
    try {
      StringBuilder url = new StringBuilder();
      url.append("ga?");
      url.append("guid=ON");
      url.append("&utmn=").append(Integer.toString((int) (Math.random() * 0x7fffffff)));
      url.append("&utmac=").append(
          _configurationService.getConfigurationValueAsString("display.googleAnalyticsSiteId", null));    

      // referrer
      HttpServletRequest request = ServletActionContext.getRequest();      
      String referer = request.getHeader("referer");	

      if (referer == null || referer.isEmpty()) {
        referer = "-";
      }

      url.append("&utmr=").append(URLEncoder.encode(referer, "UTF-8"));

      // event tracking
      String label = getQ();	      
      
      if(label == null) {
        label = "";
      }
      
      label += " [";
      label += _searchResults.size();	      

      if(_location != null) {
        label += " - with location";
      }
      
      label += "]";
      label = label.trim();

      String action = "Unknown";
      if(_searchResults != null && !_searchResults.isEmpty()) {
        if(_searchResults.getTypeOfResults().equals("RouteItem")) {
          action = "Region Search";

        } else if(_searchResults.getTypeOfResults().equals("RouteResult")) {
          action = "Route Search";

        } else if(_searchResults.getTypeOfResults().equals("LocationResult")) {
          action = "Location Disambiguation";

        } else if(_searchResults.getTypeOfResults().equals("StopResult")) {
          if(_searchResults.size() > 1) {
            action = "Intersection Search";
          } else {
            action = "Stop Search";
          }          
        }	    	  
      }	else {
        if(getQueryIsEmpty()) {
          action = "Home";
        } else {
          action = "No Search Results";	    		  
        }
      }

      // page view on homepage hit, "event" for everything else.
      if(action.equals("Home")) {
        url.append("&utmp=/m/index");
      } else {
        url.append("&utmt=event&utme=5(Mobile Web*" + action + "*" + label + ")");	    	  
      }

      return url.toString().replace("&", "&amp;"); 
    } catch(Exception e) {
      return null;
    }
  }

  public String getQ() {
    if((_q == null || _q.isEmpty()) && _location != null)
      return CURRENT_LOCATION_TEXT;
    else
      return _q;
  }
  
  public String getL() {
    if(_location != null) 
      return _location.getLat() + "," + _location.getLon();
    else
      return null;
  }
  
  public String getCacheBreaker() {
	  return String.valueOf((int)Math.ceil((Math.random() * 100000)));
  }
  
  public String getLastUpdateTime() {
    return DateFormat.getTimeInstance().format(_factory.getLastUpdateTime());
  } 
  
  public boolean getQueryIsEmpty() {
    return (_q == null || _q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT))) 
        && _location == null;
  }
  
  public String getResultType() {
    return _searchResults.getTypeOfResults();
  }
  
  public List<SearchResult> getSearchResults() {
    return _searchResults;
  }
  
  public String getTitle() {
    String title = this._q;
	
    if(_searchResults.size() == 1) {
      SearchResult result = _searchResults.get(0);
 
      if(result != null) {
        title = result.getName() + " (" + title + ")";
      }
    }
	
    return title;
  }

}
