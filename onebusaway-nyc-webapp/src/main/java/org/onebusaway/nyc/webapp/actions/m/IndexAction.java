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
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.GeocodeResult;
import org.onebusaway.nyc.webapp.actions.m.model.RouteAtStop;
import org.onebusaway.nyc.webapp.actions.m.model.RouteResult;
import org.onebusaway.nyc.webapp.actions.m.model.StopResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.xwork.StringEscapeUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private SearchService _searchService;

  private SearchResultCollection _results = new SearchResultCollection();
  
  private boolean _resultsOriginatedFromGeocode = false;
  
  private String _q = null;
  
  private CoordinatePoint _location = null;
  
  private String _type = null;
  
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
  
  public void setT(String type) {
	  this._type = type;
  }

  public String execute() throws Exception {
    if(_q == null)
      return SUCCESS;

    SearchResultFactory factory = new SearchResultFactoryImpl(_nycTransitDataService, _realtimeService, _configurationService);
    
    // empty query with location means search for stops near current location
    if(_location != null && _q.isEmpty()) {
    	if (_type.equals("stops")) {
    		_results = _searchService.findStopsNearPoint(_location.getLat(), _location.getLon(), factory, _results.getRouteIdFilter());
    	} else {
    		_results = _searchService.findRoutesStoppingNearPoint(_location.getLat(), _location.getLon(), factory);
    	}

    } else {
      if(_q.isEmpty()) {
        return SUCCESS;
      }

      _results = _searchService.getSearchResults(_q, factory);

      // do a bit of a hack with location matches--since we have no map to show locations on,
      // find things that are actionable near/within/etc. the result
      if(_results.getMatches().size() == 1 && _results.getResultType().equals("GeocodeResult")) {
    	  
    	  this._resultsOriginatedFromGeocode = true;
    	  GeocodeResult result = (GeocodeResult)_results.getMatches().get(0);
                
        // if we got a region back, list routes that pass through it
        if(result.getIsRegion()) {
          _results = _searchService.findRoutesStoppingWithinRegion(result.getBounds(), factory);

        // if we got a location (point) back, find stops nearby
        } else {
          _results = 
              _searchService.findStopsNearPoint(result.getLatitude(), result.getLongitude(), factory, _results.getRouteIdFilter());
        }  
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
      } else {
        if(getQueryIsEmpty()) {
          action = "Home";
        } else {
          action = "No Search Results";           
        }
      }

      //url.append("&utmt=event&utme=5(Mobile Web*" + action + "*" + label + ")");          
      url.append("&utmp=/m/index#" + action + "/" + label);

      return url.toString().replace("&", "&amp;"); 
    } catch(Exception e) {
      return null;
    }
  }
  
  public String getQ() {
	if(_q == null || _q.isEmpty()) {
	  return null;
    } else {
      return StringEscapeUtils.escapeHtml(_q.replace("&amp;", "&"));
    }
  }
  
  public String getL() {
    if(_location != null) 
      return _location.getLat() + "," + _location.getLon();
    else
      return null;
  }
  
  public String getT() {
	  return this._type;
  }
  
  public String getRouteColors() {
    Set<String> routeColors = new HashSet<String>();
    for(SearchResult _result : _results.getMatches()) {
      RouteResult result = (RouteResult)_result;
      routeColors.add(result.getColor());
    }

    return StringUtils.join(routeColors, ",");
  }
  
  public String getCacheBreaker() {
	return String.valueOf(System.currentTimeMillis());
  }
  
  public boolean getQueryIsEmpty() {
    return (_q == null || _q.isEmpty()) 
        && _location == null;
  }
  
  public String getLastUpdateTime() {
    return DateFormat.getTimeInstance().format(new Date());
  } 
  
  public String getResultType() {
    return _results.getResultType();
  }
  
  public Set<String> getUniqueServiceAlertsForResults() {
    Set<String> uniqueServiceAlerts = new HashSet<String>();
    
    for(SearchResult _result : _results.getMatches()) {
      if(_results.getResultType().equals("RouteResult")) {
        RouteResult result = (RouteResult)_result;
        uniqueServiceAlerts.addAll(result.getServiceAlerts());
        
      } else if(_results.getResultType().equals("StopResult")) {
        StopResult result = (StopResult)_result;        
        for(RouteAtStop route : result.getAllRoutesAvailable()) {
          uniqueServiceAlerts.addAll(route.getServiceAlerts());
        }
      }
    }
    
    return uniqueServiceAlerts;
  }
  
  public SearchResultCollection getResults() {
    return _results;
  }
  
  public boolean getResultsOriginatedFromGeocode() {
	  return _resultsOriginatedFromGeocode;
  }
  
  public String getTitle() {
    if(!getQueryIsEmpty()) {
    	if (this._q != null && !this._q.isEmpty())
    		return ": " + this._q;
    	else
    		return "";
    } else {
      return "";
    }
  }

}
