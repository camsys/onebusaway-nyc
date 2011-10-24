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

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.impl.sort.SearchResultComparator;
import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItemWithStops;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebLocationSearchResult;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebModelFactory;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebRouteDestinationItem;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebRouteItem;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebStopItem;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  private static final String GA_ACCOUNT = "UA-XXXXXXXX-X";

  private List<SearchResult> _searchResults = new ArrayList<SearchResult>();

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private RouteSearchService _routeSearchService;

  @Autowired
  private StopSearchService _stopSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;

  private long _lastUpdateTime;
  
  private String _q;

  public void setQ(String q) {
    this._q = q.trim();
  }

  public String getResultType() {
    if(_searchResults.size() > 0)
      return _searchResults.get(0).getType();
    else
      return null;
  }
  
  public List<SearchResult> getSearchResults() {
    injectRealtimeData();
    return _searchResults;
  }
  
  public String execute() throws Exception {
    if(_q == null)
      return SUCCESS;

    _q = _q.trim();
    if(_q.isEmpty())
      return SUCCESS;

    MobileWebModelFactory factory = new MobileWebModelFactory();
    _stopSearchService.setModelFactory(factory);
    _routeSearchService.setModelFactory(factory);
    
    // try as stop ID
    _searchResults.addAll(_stopSearchService.makeResultFor(_q));

    // try as route ID
    if(_searchResults.size() == 0) {
      _searchResults.addAll(_routeSearchService.makeResultFor(_q));
    }

    // nothing? geocode it!
    if(_searchResults.size() == 0) {
      _searchResults.addAll(generateResultsFromGeocode(_q));        
    }

    Collections.sort(_searchResults, new SearchResultComparator());

    return SUCCESS;
  }

  private void injectRealtimeData() {
    for(SearchResult result : _searchResults) {
      // stop centric view
      if(result.getType().equals("stopResult")) {
        StopSearchResult stopSearchResult = (StopSearchResult)result;
        for(RouteItem route : stopSearchResult.getRoutesAvailable()) {
          for(RouteDestinationItem _destination : route.getDestinations()) {
            MobileWebRouteDestinationItem destination = (MobileWebRouteDestinationItem)_destination;

            // service alerts
            List<NaturalLanguageStringBean> serviceAlerts = 
                _realtimeService.getServiceAlertsForStop(stopSearchResult.getStopId());

            destination.setServiceAlerts(serviceAlerts);

            // distance aways
            List<DistanceAway> distanceAways = 
                _realtimeService.getDistanceAwaysForStopAndHeadsign(stopSearchResult.getStopId(), destination.getHeadsign());
            
            List<String> distanceAwayStrings = new ArrayList<String>();
            for(DistanceAway distanceAway : distanceAways) {
              distanceAwayStrings.add(getStringForDistanceAway(distanceAway, false));
              _lastUpdateTime = Math.max(distanceAway.getUpdateTimestamp().getTime(), _lastUpdateTime);
            }
            
            destination.setDistanceAways(distanceAwayStrings);
          }
        }
        
      // route centric view
      } else if(result.getType().equals("routeResult")) {
        RouteSearchResult routeSearchResult = (RouteSearchResult)result;
        for(RouteDestinationItemWithStops destination : routeSearchResult.getDestinations()) {
          for(StopItem _stop : destination.getStops()) {
            MobileWebStopItem stop = (MobileWebStopItem)_stop;
            
            // service alerts
            List<NaturalLanguageStringBean> serviceAlerts = 
                _realtimeService.getServiceAlertsForStop(stop.getStopId());

            stop.setServiceAlerts(serviceAlerts);

            // distance aways
            List<DistanceAway> distanceAways = 
                _realtimeService.getDistanceAwaysForStopAndHeadsign(stop.getStopId(), destination.getHeadsign());

            StringBuilder sb = new StringBuilder();
            for(DistanceAway distanceAway : distanceAways) {
              // hide arrivals that are further than 1 stop--those will appear as part of the next stop!
              if(distanceAway.getStopsAway() > 0)
                continue;
              
              if(sb.length() > 0)
                sb.append(",");

              sb.append(getStringForDistanceAway(distanceAway, true));
              _lastUpdateTime = Math.max(distanceAway.getUpdateTimestamp().getTime(), _lastUpdateTime);
            }

            stop.setDistanceAwayString(sb.toString());
          }
        }
        
      }
    }
    
  }
  
  private String getStringForDistanceAway(DistanceAway distanceAway, boolean routeCentricView) {
    int staleDataTimeout = 
        _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);

    String s = distanceAway.getPresentableDistance();
    String subpart = new String();

    // at terminal
    if(!routeCentricView && distanceAway.isAtTerminal()) {
      subpart += "at terminal";
    }

    // old
    if(new Date().getTime() - distanceAway.getUpdateTimestamp().getTime() > 1000 * staleDataTimeout) {
      if(subpart.length() > 0)
        subpart += ", ";

      subpart += "old data";
    }

    if(subpart.length() > 0) {
      s += " (" + subpart + ")";
    }

    return s;
  }
  
  private List<SearchResult> generateResultsFromGeocode(String q) {
    List<SearchResult> results = new ArrayList<SearchResult>();
    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(q);

    // one location: if region, show routes, if point, show stops nearby
    if(geocoderResults.size() == 1) {
      NycGeocoderResult result = geocoderResults.get(0);

      if(result.isRegion()) {
        List<RouteItem> routeItems = _routeSearchService.itemsForLocation(result.getBounds());
        for(RouteItem _routeItem : routeItems) {
          MobileWebRouteItem routeItem = (MobileWebRouteItem)_routeItem;
          results.add(routeItem);
        }
      } else {
        results.addAll(_stopSearchService.resultsForLocation(result.getLatitude(), result.getLongitude()));
      }
      
      return results;

    // location disambiguation w/ nearby routes
    } else {
      for(NycGeocoderResult result : geocoderResults) {
        List<RouteItem> nearbyRoutes = null;
        if(result.isRegion())
          nearbyRoutes = _routeSearchService.itemsForLocation(result.getBounds());
        else
          nearbyRoutes = _routeSearchService.itemsForLocation(result.getLatitude(), result.getLongitude());
        
        results.add(new MobileWebLocationSearchResult(result, nearbyRoutes));
      }
    }

    return results;
  }  
  
  // Adapted from http://code.google.com/mobile/analytics/docs/web/#jsp
  public String getGoogleAnalyticsTrackingUrl() {
	  try {
	      StringBuilder url = new StringBuilder();
	      url.append("/m/ga?");
	      url.append("utmac=").append(GA_ACCOUNT);
	      url.append("&utmn=").append(Integer.toString((int) (Math.random() * 0x7fffffff)));

	      // referer
	      HttpServletRequest request = ServletActionContext.getRequest();      
	      String referer = request.getHeader("referer");
	
	      if (referer == null || "".equals(referer)) {
	        referer = "-";
	      }
	      url.append("&utmr=").append(URLEncoder.encode(referer, "UTF-8"));

	      // event tracking
	      String label = getQ();	      
	      if(label == null) {
	    	  label = "";
	      }
	      
	      String action = new String("Unknown");
	      if(_searchResults != null && !_searchResults.isEmpty()) {
	    	  SearchResult firstResult = _searchResults.get(0);	    	  
	    	  if(firstResult.getType().equals("route")) {
	    		  action = "Route Search";
	    	  } else if(firstResult.getType().equals("stop")) {
	    		  if(_searchResults.size() > 1) {
	    			  action = "Intersection Search";
	    		  } else {
	    			  action = "Stop Search";
	    		  }
	    	  }	    	  
	      }	else {
	    	  if(_q == null || _q.isEmpty()) {
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
	      
	      // misc.
	      url.append("&guid=ON");
	      
	      return url.toString().replace("&", "&amp;"); 
	  } catch(Exception e) {
		  return null;
	  }
  }
    
  public String getQ() {
    return _q;
  }

  public String getCacheBreaker() {
	  return String.valueOf(Math.ceil((Math.random() * 100000)));
  }
  
  public String getLastUpdateTime() {
    return DateFormat.getTimeInstance().format(_lastUpdateTime);
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
