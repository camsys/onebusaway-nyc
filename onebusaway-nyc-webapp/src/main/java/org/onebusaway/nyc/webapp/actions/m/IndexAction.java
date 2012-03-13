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
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.lang.xwork.StringEscapeUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;

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

    // empty query with location means search for current location
    if(_location != null && (_q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT)))) {
      // location search
      
    } else {
      if(_q.isEmpty()) {
        return SUCCESS;
      }

      _results = _searchService.getSearchResults(_q, new SearchResultFactoryImpl(_transitDataService, 
          _searchService, _scheduledServiceService, _realtimeService, _configurationService));
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
      
      label += " [";
      label += _results.getMatches().size();	      

      if(_location != null) {
        label += " - with location";
      }
      
      label += "]";
      label = label.trim();

      String action = "Unknown";
//      if(_searchResults != null && !_searchResults.isEmpty()) {
//        if(_searchResults.getTypeOfResults().equals("RouteItem")) {
//          action = "Region Search";
//
//        } else if(_searchResults.getTypeOfResults().equals("RouteResult")) {
//          action = "Route Search";
//
//        } else if(_searchResults.getTypeOfResults().equals("LocationResult")) {
//          action = "Location Disambiguation";
//
//        } else if(_searchResults.getTypeOfResults().equals("StopResult")) {
//          if(_searchResults.size() > 1) {
//            action = "Intersection Search";
//          } else {
//            action = "Stop Search";
//          }          
//        }	    	  
//      }	else {
//        if(getQueryIsEmpty()) {
//          action = "Home";
//        } else {
//          action = "No Search Results";	    		  
//        }
//      }

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
  
  public String getLastUpdateTime() {
    return "FIXME";
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
    String title = this._q;
	
//    if(_results.getMatches().size() == 1) {
//      SearchResult result = _searchResults.get(0);
// 
//      if(result != null) {
//        title = result.getName() + " (" + title + ")";
//      }
//    }
//	
    return title;
  }

}
