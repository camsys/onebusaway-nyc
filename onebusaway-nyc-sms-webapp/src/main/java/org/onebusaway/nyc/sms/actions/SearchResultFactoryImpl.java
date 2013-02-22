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
package org.onebusaway.nyc.sms.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.impl.search.AbstractSearchResultFactoryImpl;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.sms.actions.model.GeocodeResult;
import org.onebusaway.nyc.sms.actions.model.RouteAtStop;
import org.onebusaway.nyc.sms.actions.model.RouteDirection;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;

public class SearchResultFactoryImpl extends AbstractSearchResultFactoryImpl {

  private static final boolean HTMLIZE_NEWLINES = false;

  private ConfigurationService _configurationService;

  private RealtimeService _realtimeService;
    
  private NycTransitDataService _nycTransitDataService;

  public SearchResultFactoryImpl(NycTransitDataService nycTransitDataService, 
      RealtimeService realtimeService, ConfigurationService configurationService) {
    _nycTransitDataService = nycTransitDataService;
    _realtimeService = realtimeService;
    _configurationService = configurationService;
  }

  @Override
  public SearchResult getRouteResultForRegion(RouteBean routeBean) { 
    return null;
  }
  
  @Override
  public SearchResult getRouteResult(RouteBean routeBean) {    
    List<RouteDirection> directions = new ArrayList<RouteDirection>();
    
    StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();

        if (!type.equals("destination"))
          continue;
        
        Boolean hasUpcomingScheduledService = 
            _nycTransitDataService.routeHasUpcomingScheduledService(System.currentTimeMillis(), routeBean.getId(), stopGroupBean.getId());

        // if there are buses on route, always have "scheduled service"
        Boolean routeHasVehiclesInService = 
      		  _realtimeService.getVehiclesInServiceForRoute(routeBean.getId(), stopGroupBean.getId());

        if(routeHasVehiclesInService) {
      	  hasUpcomingScheduledService = true;
        }

        // service alerts for this route + direction
        List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
        List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
        populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, HTMLIZE_NEWLINES);

        directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, null, serviceAlertDescriptions));
      }
    }
    
    return new RouteResult(routeBean, directions);
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean, Set<RouteBean> routeFilter) {
    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    boolean matchesRouteIdFilter = false;
    
    HashMap<String, HashMap<Double, String>> distanceAwayStringsByDistanceFromStopAndRouteAndDirection = 
        getDistanceAwayStringsForStopByDistanceFromStopAndRouteAndDirection(stopBean);
    
    for(RouteBean routeBean : stopBean.getRoutes()) {
      if((routeFilter == null || routeFilter.isEmpty()) || routeFilter != null && routeFilter.contains(routeBean)) {
        matchesRouteIdFilter = true;
      }
      
      StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());
      
      List<RouteDirection> directions = new ArrayList<RouteDirection>();
      List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
      for (StopGroupingBean stopGroupingBean : stopGroupings) {
        for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
          NameBean name = stopGroupBean.getName();
          String type = name.getType();

          if (!type.equals("destination"))
            continue;
        
          // filter out route directions that don't stop at this stop
          if(!stopGroupBean.getStopIds().contains(stopBean.getId()))
            continue;

          HashMap<Double, String> distanceAwayStringsByDistanceFromStop = 
              distanceAwayStringsByDistanceFromStopAndRouteAndDirection.get(routeBean.getId() + "_" + stopGroupBean.getId());
          
          Boolean hasUpcomingScheduledService = 
              _nycTransitDataService.stopHasUpcomingScheduledService(System.currentTimeMillis(), stopBean.getId(), routeBean.getId(), stopGroupBean.getId());

          // if there are buses on route, always have "scheduled service"
          if(distanceAwayStringsByDistanceFromStop != null && !distanceAwayStringsByDistanceFromStop.isEmpty()) {
        	  hasUpcomingScheduledService = true;
          }

          // service alerts for this route + direction
          List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
          List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
          populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, HTMLIZE_NEWLINES);
          
          directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, distanceAwayStringsByDistanceFromStop, serviceAlertDescriptions));
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
      routesAtStop.add(routeAtStop);
    }

    return new StopResult(stopBean, routesAtStop, matchesRouteIdFilter);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<RouteBean> routeFilter) {
    return new GeocodeResult(geocodeResult);   
  }

  /*** 
   * PRIVATE METHODS
   */
  private HashMap<String, HashMap<Double, String>> getDistanceAwayStringsForStopByDistanceFromStopAndRouteAndDirection(StopBean stopBean) {
    HashMap<String, HashMap<Double, String>> result = new HashMap<String, HashMap<Double, String>>();

    // stop visits
    List<MonitoredStopVisitStructure> visitList = 
      _realtimeService.getMonitoredStopVisitsForStop(stopBean.getId(), 0);  
    
    for(MonitoredStopVisitStructure visit : visitList) {
      // on detour? don't show it. 
      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
      if(monitoredCall == null)
        continue;

      String distance = getPresentableDistance(visit.getMonitoredVehicleJourney(),
    		  visit.getRecordedAtTime().getTime(), true);

      String timePrediction = getPresentableTime(visit.getMonitoredVehicleJourney(),
    		  visit.getRecordedAtTime().getTime(), true);

      SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
      SiriDistanceExtension distanceExtension = wrapper.getDistances();    

      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      String key = routeId + "_" + directionId;

      HashMap<Double, String> map = result.get(key);
      if(map == null) {
        map = new HashMap<Double,String>();
      }
      
      if(timePrediction != null) {
        map.put(distanceExtension.getDistanceFromCall(), timePrediction);
      } else {
        map.put(distanceExtension.getDistanceFromCall(), distance);
      }

      result.put(key, map);
    }
    
    return result;
  }
  
  private String getPresentableTime(
		  MonitoredVehicleJourneyStructure journey, long updateTime,
	      boolean isStopContext) {

	  NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
	  MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
	  
	  // only show predictions in stop-level contexts
	  if(!isStopContext) {
		  return null;
	  }
	  
	  // if data is old, no predictions
	  int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
	  long age = (System.currentTimeMillis() - updateTime) / 1000;

	  if (age > staleTimeout) {
		  return null;
	  }
	  
	  if(monitoredCall.getExpectedArrivalTime() != null) {
		  long predictedArrival = monitoredCall.getExpectedArrivalTime().getTime();

		  SiriExtensionWrapper wrapper = (SiriExtensionWrapper) monitoredCall.getExtensions().getAny();
		  SiriDistanceExtension distanceExtension = wrapper.getDistances();
		  String distance = distanceExtension.getPresentableDistance();
		  
		  double minutes = Math.floor((predictedArrival - updateTime) / 60 / 1000);
		  String timeString = minutes + " min" + ((Math.abs(minutes) != 1) ? "s." : ".");
				  
		  // if wrapped, only show prediction, if not wrapped, show both
		  if(progressStatus != null && progressStatus.getValue().contains("prevTrip")) {
		    return timeString;
		  } else {
		    return timeString + ", " + distance;
		  }
	  }
	  
	  return null;
  }	  
  
  private String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime, boolean isStopContext) {
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
    SiriDistanceExtension distanceExtension = wrapper.getDistances();    
    
    String message = "";    
    String distance = _realtimeService.getPresentationService().getPresentableDistance(distanceExtension, "arriving", "stop", "stops", "mile", "miles", "");

    NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
    
    // wrapped label only appears in stop results
    if(isStopContext) {    	
    	if(progressStatus != null && progressStatus.getValue().contains("layover")) {
    	   	if(journey.getOriginAimedDepartureTime() != null) {
        		DateFormat formatter = new SimpleDateFormat("h:mm");

        		if(journey.getOriginAimedDepartureTime().getTime() < new Date().getTime()) {
            		message += "@term.";
        		} else {
            		message += "@term. sched. dep. " + 
            				formatter.format(journey.getOriginAimedDepartureTime());
        		}
        	} else {
        		message += "@term.";
        	}
    	} else if(progressStatus != null && progressStatus.getValue().contains("prevTrip")) {
    		message += "+layover@term.";
    	}
    }
    
    int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);    
    long age = (System.currentTimeMillis() - updateTime) / 1000;

    if(age > staleTimeout) {
      if(message.length() > 0) {
        message += ", ";
      }
      
      message += "old";
    }

    if(message.length() > 0)
      return distance + "(" + message + ")";
    else
      return distance;
  }
}
