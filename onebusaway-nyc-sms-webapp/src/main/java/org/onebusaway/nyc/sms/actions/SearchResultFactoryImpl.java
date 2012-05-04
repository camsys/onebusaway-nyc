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

import java.util.ArrayList;
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
import uk.org.siri.siri.VehicleActivityStructure;

public class SearchResultFactoryImpl extends AbstractSearchResultFactoryImpl {

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
        List<VehicleActivityStructure> vehiclesOnRoute = 
        		_realtimeService.getVehicleActivityForRoute(routeBean.getId(), stopGroupBean.getId(), 0);
        
        if(!vehiclesOnRoute.isEmpty()) {
        	hasUpcomingScheduledService = true;
        }
        
        // service alerts for this route + direction
        List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
        List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
        populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);

        directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, null, serviceAlertDescriptions));
      }
    }
    
    return new RouteResult(routeBean, directions);
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean, Set<String> routeIdFilter) {
    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    
    for(RouteBean routeBean : stopBean.getRoutes()) {
      if(routeIdFilter != null && !routeIdFilter.isEmpty() && !routeIdFilter.contains(routeBean.getId())) {
        continue;
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
              getDistanceAwayStringsForStopAndRouteAndDirectionByDistanceFromStop(stopBean, routeBean, stopGroupBean);
          
          Boolean hasUpcomingScheduledService = 
              _nycTransitDataService.stopHasUpcomingScheduledService(System.currentTimeMillis(), stopBean.getId(), routeBean.getId(), stopGroupBean.getId());

          // if there are buses on route, always have "scheduled service"
          if(!distanceAwayStringsByDistanceFromStop.isEmpty()) {
        	  hasUpcomingScheduledService = true;
          }

          // service alerts for this route + direction
          List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
          List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
          populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);
          
          directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, distanceAwayStringsByDistanceFromStop, serviceAlertDescriptions));
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
      routesAtStop.add(routeAtStop);
    }

    return new StopResult(stopBean, routesAtStop);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<String> routeIdFilter) {
    return new GeocodeResult(geocodeResult);   
  }

  /*** 
   * PRIVATE METHODS
   */
  private HashMap<Double, String> getDistanceAwayStringsForStopAndRouteAndDirectionByDistanceFromStop(StopBean stopBean, RouteBean routeBean, StopGroupBean stopGroupBean) {
    HashMap<Double, String> result = new HashMap<Double, String>();

    // stop visits
    List<MonitoredStopVisitStructure> visitList = 
      _realtimeService.getMonitoredStopVisitsForStop(stopBean.getId(), 0);  
    
    for(MonitoredStopVisitStructure visit : visitList) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      if(!routeBean.getId().equals(routeId))
        continue;

      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      if(!stopGroupBean.getId().equals(directionId))
        continue;

      // on detour?
      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
      if(monitoredCall == null) {
        continue;
      }

      if(result.size() < 3) {
        SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
        SiriDistanceExtension distanceExtension = wrapper.getDistances();    

        result.put(distanceExtension.getDistanceFromCall(), getPresentableDistance(visit.getMonitoredVehicleJourney(), visit.getRecordedAtTime().getTime(), true));
      }
    }
    
    return result;
  }
  
  private String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime, boolean isStopContext) {
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
    SiriDistanceExtension distanceExtension = wrapper.getDistances();    
    
    String message = "";    
    String distance = _realtimeService.getPresentationService().getPresentableDistance(distanceExtension, "arriving", "stop", "stops", "mi.", "mi.", "");

    // at terminal label only appears in stop results
    NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
    if(isStopContext && progressStatus != null && progressStatus.getValue().equals("layover")) {
      message += "@term.";
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
