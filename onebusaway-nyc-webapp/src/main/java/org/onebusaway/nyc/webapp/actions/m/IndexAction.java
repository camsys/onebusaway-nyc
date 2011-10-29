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
import org.onebusaway.nyc.presentation.impl.sort.SearchResultComparator;
import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebLocationSearchResult;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebRouteDestinationItem;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebSearchModelFactory;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebStopSearchResult;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final String currentLocationText = "(Current Location)";
  
  private static final long serialVersionUID = 1L;
  
  private static final String GA_ACCOUNT = "UA-XXXXXXXX-X";

  private List<SearchResult> _searchResults = new ArrayList<SearchResult>();

  private long lastUpdateTime = System.currentTimeMillis();
  
  private String _q;
  
  private CoordinatePoint _location;

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private RouteSearchService _routeSearchService;

  @Autowired
  private StopSearchService _stopSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;
  
  public void setQ(String q) {
    this._q = q.trim();
  }

  public void setL(String location) {
    String[] locationParts = location.split(",");
    if(locationParts.length == 2) {
      _location = new CoordinatePoint(Double.parseDouble(locationParts[0]), 
            Double.parseDouble(locationParts[1]));
    }
  }
  
  public String execute() throws Exception {
    if(_q == null)
      return SUCCESS;

    MobileWebSearchModelFactory factory = new MobileWebSearchModelFactory();
    _stopSearchService.setModelFactory(factory);
    _routeSearchService.setModelFactory(factory);
    
    _q = _q.trim();
      
    // empty query with location means search for current location
    if(_location != null && (_q.isEmpty() || (_q != null && _q.equals(currentLocationText)))) {
      _searchResults.addAll(_stopSearchService.resultsForLocation(_location.getLat(), _location.getLon()));        

    } else {
      if(_q.isEmpty())
        return SUCCESS;
      
      // try as stop ID
      _searchResults.addAll(_stopSearchService.resultsForQuery(_q));

      // try as route ID
      if(_searchResults.size() == 0) {
        _searchResults.addAll(_routeSearchService.resultsForQuery(_q));
      }

      // nothing? geocode it!
      if(_searchResults.size() == 0) {
        _searchResults.addAll(generateResultsFromGeocode(_q));        
      }
    }

    injectRealtimeDataIntoResults(_searchResults);
      
    Collections.sort(_searchResults, new SearchResultComparator());

    return SUCCESS;
  }
  
  private void injectRealtimeDataIntoResults(List<SearchResult> searchResults) {
    for(SearchResult searchResult : searchResults) {
      if(searchResult instanceof StopResult) {
        injectRealtimeData((StopResult)searchResult);
      } else if(searchResult instanceof RouteResult) {
        injectRealtimeData((RouteResult)searchResult);
      }
    }
  }
  
  private StopResult injectRealtimeData(StopResult stopSearchResult) {
    for(RouteResult route : stopSearchResult.getRoutesAvailable()) {
      for(RouteDestinationItem _destination : route.getDestinations()) {
        MobileWebRouteDestinationItem destination = (MobileWebRouteDestinationItem)_destination;

        // service alerts
        List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForStop(stopSearchResult.getStopId());
        destination.setServiceAlerts(serviceAlerts);

        // distance aways
        List<DistanceAway> distanceAways = _realtimeService.getDistanceAwaysForStopAndDestination(stopSearchResult.getStopId(), destination);

        List<String> distanceAwayStrings = new ArrayList<String>();

        for(DistanceAway distanceAway : distanceAways) {
          Long thisLastUpdateTime = distanceAway.getUpdateTimestamp();
          if(thisLastUpdateTime != null && thisLastUpdateTime > lastUpdateTime) {
            lastUpdateTime = thisLastUpdateTime;
          }

          distanceAwayStrings.add(distanceAway.toString());
        }

        destination.setDistanceAwayStrings(distanceAwayStrings);
      }
    }    

    return stopSearchResult;
  }

  private RouteResult injectRealtimeData(RouteResult routeSearchResult) {
    for(RouteDestinationItem destination : routeSearchResult.getDestinations()) {
      if(destination.getStops() == null)
        continue;
      
      for(StopResult _stop : destination.getStops()) {
        MobileWebStopSearchResult stop = (MobileWebStopSearchResult)_stop;

        List<DistanceAway> distanceAways = _realtimeService.getDistanceAwaysForStopAndDestination(stop.getStopId(), destination);

        StringBuilder sb = new StringBuilder();

        for(DistanceAway distanceAway : distanceAways) {
          // hide arrivals that are further than 1 stop--those will appear as part of the next stop!
          if(distanceAway.getStopsAway() > 0)
            continue;

          Long thisLastUpdateTime = distanceAway.getUpdateTimestamp();
          if(thisLastUpdateTime != null && thisLastUpdateTime > lastUpdateTime) {
            lastUpdateTime = thisLastUpdateTime;
          }

          if(sb.length() > 0)
            sb.append(", ");

          sb.append(distanceAway.toString());
        }

        stop.setDistanceAwayString(sb.toString());
      }
    }

    return routeSearchResult;       
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
        List<RouteResult> nearbyRoutes = null;
        if(result.isRegion())
          nearbyRoutes = _routeSearchService.resultsForLocation(result.getBounds());
        else
          nearbyRoutes = _routeSearchService.resultsForLocation(result.getLatitude(), result.getLongitude());
        
        MobileWebLocationSearchResult locationSearchResult = new MobileWebLocationSearchResult();
        locationSearchResult.setGeocoderResult(result);
        locationSearchResult.setNearbyRoutes(nearbyRoutes);

        results.add(locationSearchResult);
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
	      
	      // misc.
	      url.append("&guid=ON");
	      
	      return url.toString().replace("&", "&amp;"); 
	  } catch(Exception e) {
		  return null;
	  }
  }
    
  public String getQ() {
    if(_location != null)
      return currentLocationText;
    else
      return _q;
  }

  public String getCacheBreaker() {
	  return String.valueOf(Math.ceil((Math.random() * 100000)));
  }
  
  public String getLastUpdateTime() {
    return DateFormat.getTimeInstance().format(lastUpdateTime);
  } 
  
  public boolean getQueryIsEmpty() {
    return (_q == null || _q.isEmpty() || (_q != null && _q.equals(currentLocationText))) 
        && _location == null;
  }
  
  public String getResultType() {
    if(_searchResults.size() > 0)
      return _searchResults.get(0).getType();
    else
      return null;
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
