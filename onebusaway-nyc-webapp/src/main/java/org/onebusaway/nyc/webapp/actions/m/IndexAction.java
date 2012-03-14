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
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.GeocodeResult;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.lang.xwork.StringEscapeUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private static final String CURRENT_LOCATION_TEXT = "(Current Location)";

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private ScheduledServiceService _scheduledServiceService;

  @Autowired
  private TransitDataService _transitDataService;

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private SearchService _searchService;

  private SearchResultCollection _results = new SearchResultCollection();
  
  private String _q = null;
  
  private CoordinatePoint _location = null;
  
  public void setQ(String q) {
    if(q != null) {
      this._q = q.trim();
    }
  }

  public void setL(String location) {
    String[] locationParts = location.split(",");

    if(locationParts.length == 2) {
      this._location = new CoordinatePoint(
          Double.parseDouble(locationParts[0]), 
          Double.parseDouble(locationParts[1]));
    }
  }

  public String execute() throws Exception {
    if(_q == null)
      return SUCCESS;

    SearchResultFactory factory = new SearchResultFactoryImpl(_transitDataService,
        _scheduledServiceService, _realtimeService, _configurationService);
    
    // empty query with location means search for current location
    if(_location != null && (_q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT)))) {
      List<SearchResult> stopsNearLocation = 
          _searchService.getStopResultsNearPoint(_location.getLat(), _location.getLon(), factory);

      for(SearchResult stopNearLocation : stopsNearLocation) {
        _results.addMatch(stopNearLocation);
      }          
    } else {
      if(_q.isEmpty()) {
        return SUCCESS;
      }

      _results = _searchService.getSearchResults(_q, factory);

      // do a bit of a hack with location matches--since we have no map to show locations on,
      // find things that are actionable near/within/etc. the result
      if(_results.getMatches().size() == 1 && _results.getResultType().equals("GeocodeResult")) {
        GeocodeResult result = (GeocodeResult)_results.getMatches().get(0);
        
        SearchResultCollection newResults = new SearchResultCollection();
        
        // if we got a region back, list routes that pass through it
        if(result.getIsRegion()) {
          List<SearchResult> routesInRegion = 
              _searchService.getRouteResultsStoppingWithinRegion(result.getBounds(), factory);

          for(SearchResult routeInRegionResult : routesInRegion) {
            newResults.addSuggestion(routeInRegionResult);
          }
          
        // if we got a location (point) back, find stops nearby
        } else {
          List<SearchResult> stopsNearLocation = 
              _searchService.getStopResultsNearPoint(result.getLatitude(), result.getLongitude(), factory);

          for(SearchResult stopNearLocation : stopsNearLocation) {
            newResults.addMatch(stopNearLocation);
          }          
        }  
        
        _results = newResults;
      }
    }

    return SUCCESS;
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
      label += " [M: " + _results.getMatches().size() + " S: " + _results.getSuggestions().size();
      if(_location != null) {
        label += " - with location";
      }      
      label += "]";
      label = label.trim();

      String action = "Unknown";
      if(_results != null && !_results.isEmpty()) {
        if(_results.getResultType().equals("RouteInRegionResult")) {
          action = "Region Search";

        } else if(_results.getResultType().equals("RouteResult")) {
          action = "Route Search";

        } else if(_results.getResultType().equals("GeocodeResult")) {
          action = "Location Disambiguation";

        } else if(_results.getResultType().equals("StopResult")) {
          action = "Stop or Intersection Search";
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
      return StringEscapeUtils.escapeHtml(_q);
  }
  
  public String getL() {
    if(_location != null) 
      return _location.getLat() + "," + _location.getLon();
    else
      return "off";
  }
  
  public String getCacheBreaker() {
	  return String.valueOf((int)Math.ceil((Math.random() * 100000)));
  }
  
  public boolean getQueryIsEmpty() {
    return (_q == null || _q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT))) 
        && _location == null;
  }
  
  public String getResultType() {
    return _results.getResultType();
  }
  
  public SearchResultCollection getResults() {
    return _results;
  }
  
  public String getTitle() {
    if(!getQueryIsEmpty()) {
      return ":" + this._q;
    } else {
      return "";
    }
  }

}
