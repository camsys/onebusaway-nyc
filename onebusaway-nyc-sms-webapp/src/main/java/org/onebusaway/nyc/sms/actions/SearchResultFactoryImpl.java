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

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.sms.actions.model.GeocodeResult;
import org.onebusaway.nyc.sms.actions.model.RouteAtStop;
import org.onebusaway.nyc.sms.actions.model.RouteDirection;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.services.TransitDataService;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SearchResultFactoryImpl implements SearchResultFactory {

  private TransitDataService _transitDataService;

  private ConfigurationService _configurationService;

  private RealtimeService _realtimeService;
    
  private ScheduledServiceService _scheduledServiceService;

  public SearchResultFactoryImpl(TransitDataService transitDataService, ScheduledServiceService scheduledServiceService, 
      RealtimeService realtimeService, ConfigurationService configurationService) {
    _transitDataService = transitDataService;
    _scheduledServiceService = scheduledServiceService;
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
    
    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();

        if (!type.equals("destination"))
          continue;
        
        Boolean hasUpcomingScheduledService = 
            _scheduledServiceService.hasUpcomingScheduledService(routeBean, stopGroupBean);

        // service alerts for this route + direction
        List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();

        List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
        for(ServiceAlertBean serviceAlertBean : serviceAlertBeans) {
          for(NaturalLanguageStringBean description : serviceAlertBean.getDescriptions()) {
            if(description.getValue() != null) {
              description.setValue(description.getValue().replace("\n", "<br/>"));
              serviceAlertDescriptions.add(description);
            }
          }
        }

        directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, null, serviceAlertDescriptions));
      }
    }
    
    return new RouteResult(routeBean, directions);
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean, Set<String> routeIdFilter) {
    StopBean stop = _transitDataService.getStop(stopBean.getId());    

    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    
    for(RouteBean routeBean : stop.getRoutes()) {
      if(routeIdFilter != null && !routeIdFilter.isEmpty() && !routeIdFilter.contains(routeBean.getId())) {
        continue;
      }
    
      StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
      
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

          List<String> arrivalsForRouteAndDirection = getDistanceAwayStringsForStopAndRouteAndDirection(stopBean, routeBean, stopGroupBean);
          
          Boolean hasUpcomingScheduledService = 
              _scheduledServiceService.hasUpcomingScheduledService(routeBean, stopGroupBean);

          // service alerts for this route + direction
          List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();

          List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
          for(ServiceAlertBean serviceAlertBean : serviceAlertBeans) {
            for(NaturalLanguageStringBean description : serviceAlertBean.getDescriptions()) {
              if(description.getValue() != null) {
                description.setValue(description.getValue().replace("\n", "<br/>"));
                serviceAlertDescriptions.add(description);
              }
            }
          }
          
          directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, arrivalsForRouteAndDirection, serviceAlertDescriptions));
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
      routesAtStop.add(routeAtStop);
    }

    return new StopResult(stop, routesAtStop);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<String> routeIdFilter) {
    return new GeocodeResult(geocodeResult);   
  }

  /*** 
   * PRIVATE METHODS
   */
  private List<String> getDistanceAwayStringsForStopAndRouteAndDirection(StopBean stopBean, RouteBean routeBean, StopGroupBean stopGroupBean) {
    List<String> result = new ArrayList<String>();

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
        result.add(getPresentableDistance(visit.getMonitoredVehicleJourney(), visit.getRecordedAtTime().getTime(), true));
      }
    }
    
    return result;
  }
  
  private String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime, boolean isStopContext) {
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
    SiriDistanceExtension distanceExtension = wrapper.getDistances();    
    
    String message = "";    
    String distance = _realtimeService.getPresentationService().getPresentableDistance(distanceExtension, "apprch.", "stop", "stops", "mi.", "mi.");

    // at terminal label only appears in stop results
    NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
    if(isStopContext && progressStatus != null && progressStatus.getValue().equals("layover")) {
      message += "at terminal";
    }
    
    int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);    
    long age = (System.currentTimeMillis() - updateTime) / 1000;

    if(age > staleTimeout) {
      if(message.length() > 0) {
        message += ", ";
      }
      
      message += "old data";
    }

    if(message.length() > 0)
      return distance + " (" + message + ")";
    else
      return distance;
  }
}
