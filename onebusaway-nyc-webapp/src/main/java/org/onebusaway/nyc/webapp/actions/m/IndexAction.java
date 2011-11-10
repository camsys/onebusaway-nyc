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
import org.onebusaway.nyc.presentation.impl.realtime.SiriDistanceExtension;
import org.onebusaway.nyc.presentation.impl.realtime.SiriExtensionWrapper;
import org.onebusaway.nyc.presentation.impl.sort.LocationSensitiveSearchResultComparator;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebLocationResult;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebRouteDestinationItem;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebSearchModelFactory;
import org.onebusaway.nyc.webapp.actions.m.model.MobileWebStopResult;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.VehicleActivityStructure;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private static final String CURRENT_LOCATION_TEXT = "(Current Location)";
  
  private static final String GA_ACCOUNT = "UA-XXXXXXXX-X";

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private RouteSearchService _routeSearchService;

  @Autowired
  private StopSearchService _stopSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;

  private List<SearchResult> _searchResults = new ArrayList<SearchResult>();

  private long lastUpdateTime = System.currentTimeMillis();
  
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

    MobileWebSearchModelFactory factory = new MobileWebSearchModelFactory();
    _stopSearchService.setModelFactory(factory);
    _routeSearchService.setModelFactory(factory);
    
    // empty query with location means search for current location
    if(_location != null && (_q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT)))) {
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

    transformSearchModels(_searchResults);
      
    Collections.sort(_searchResults, new LocationSensitiveSearchResultComparator(_location));

    return SUCCESS;
  }
  
  /**
   * PRIVATE HELPER METHODS
   */
  private void transformSearchModels(List<SearchResult> searchResults) {
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
        List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForStop(
            stopSearchResult.getStopId());
        destination.setServiceAlerts(serviceAlerts);

        // stop visits
        List<MonitoredStopVisitStructure> visits = _realtimeService.getMonitoredStopVisitsForStop(
            stopSearchResult.getStopId(), false);

        List<String> distanceAwayStrings = new ArrayList<String>();
        for(MonitoredStopVisitStructure visit : visits) {
          String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
          String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
          if(!route.getRouteId().equals(routeId) || !destination.getDirectionId().equals(directionId))
            continue;

          // find latest update time across all realtime data
          Long thisLastUpdateTime = visit.getRecordedAtTime().getTime();
          if(thisLastUpdateTime != null && thisLastUpdateTime > lastUpdateTime) {
            lastUpdateTime = thisLastUpdateTime;
          }
          
          MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
          if(monitoredCall == null) 
            continue;

          SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
          SiriDistanceExtension distanceExtension = wrapper.getDistances();
          distanceAwayStrings.add(distanceExtension.getPresentableDistance());
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
      
      List<VehicleActivityStructure> journeyList = 
          _realtimeService.getVehicleActivityForRoute(routeSearchResult.getRouteId(), null, false);
      
      // build map of stop IDs to list of distance strings
      Map<String, ArrayList<String>> stopIdToDistanceStringMap = new HashMap<String, ArrayList<String>>();      

      for(VehicleActivityStructure journey : journeyList) {
        MonitoredCallStructure monitoredCall = journey.getMonitoredVehicleJourney().getMonitoredCall();
        if(monitoredCall == null) 
          continue;

        // find latest update time across all realtime data
        Long thisLastUpdateTime = journey.getRecordedAtTime().getTime();
        if(thisLastUpdateTime != null && thisLastUpdateTime > lastUpdateTime) {
          lastUpdateTime = thisLastUpdateTime;
        }

        SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
        SiriDistanceExtension distanceExtension = wrapper.getDistances();
        String stopId = monitoredCall.getStopPointRef().getValue();

        ArrayList<String> distances = stopIdToDistanceStringMap.get(stopId);
        if(distances == null)
          distances = new ArrayList<String>();

        distances.add(distanceExtension.getPresentableDistance());

        stopIdToDistanceStringMap.put(stopId, distances);        
      }
      
      // fold the list of distance strings into the stop list from the route result
      for(StopResult _stop : destination.getStops()) {
        MobileWebStopResult stop = (MobileWebStopResult)_stop;

        StringBuilder sb = new StringBuilder();        

        List<String> distancesForThisStop = stopIdToDistanceStringMap.get(stop.getStopId());
        if(distancesForThisStop != null) {
          for(String distance : distancesForThisStop) {
            if(sb.length() > 0) {
              sb.append(", ");
            }

            sb.append(distance);
          }
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
        MobileWebLocationResult locationSearchResult = new MobileWebLocationResult();
        locationSearchResult.setGeocoderResult(result);

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
	      url.append("/m/ga?");
	      url.append("utmac=").append(GA_ACCOUNT);
	      url.append("&utmn=").append(Integer.toString((int) (Math.random() * 0x7fffffff)));

	      // referrer
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
    return DateFormat.getTimeInstance().format(lastUpdateTime);
  } 
  
  public boolean getQueryIsEmpty() {
    return (_q == null || _q.isEmpty() || (_q != null && _q.equals(CURRENT_LOCATION_TEXT))) 
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
