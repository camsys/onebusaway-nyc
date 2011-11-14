package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.impl.realtime.SiriDistanceExtension;
import org.onebusaway.nyc.presentation.impl.realtime.SiriExtensionWrapper;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.VehicleActivityStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobileWebPresentationModelFactory extends DefaultPresentationModelFactory {

  private RealtimeService _realtimeService;
  
  private long _lastUpdateTime = System.currentTimeMillis();
  
  public MobileWebPresentationModelFactory(RealtimeService realtimeService) {
    _realtimeService = realtimeService;
  }

  public long getLastUpdateTime() {
    return _lastUpdateTime;
  }
  
  @Override
  public StopResult getStopModelForRoute(StopBean stopBean) {
    return new MobileWebStopResult(stopBean, null);
  }
  
  @Override
  public RouteDestinationItem getRouteDestinationModelForStop(StopGroupBean group, RouteBean route, StopBean stop) {
    MobileWebRouteDestinationItem destination = new MobileWebRouteDestinationItem(group, null);

    // service alerts
    List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForStop(stop.getId());
    destination.setServiceAlerts(serviceAlerts);

    // stop visits
    List<MonitoredStopVisitStructure> visits = _realtimeService.getMonitoredStopVisitsForStop(stop.getId(), false);

    List<String> distanceAwayStrings = new ArrayList<String>();
    for(MonitoredStopVisitStructure visit : visits) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      if(!route.getId().equals(routeId) || !destination.getDirectionId().equals(directionId))
        continue;

      // find latest update time across all realtime data
      Long thisLastUpdateTime = visit.getRecordedAtTime().getTime();
      if(thisLastUpdateTime != null && thisLastUpdateTime > _lastUpdateTime) {
        _lastUpdateTime = thisLastUpdateTime;
      }

      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
      if(monitoredCall == null) 
        continue;

      SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
      SiriDistanceExtension distanceExtension = wrapper.getDistances();
      distanceAwayStrings.add(distanceExtension.getPresentableDistance());
    }

    destination.setDistanceAwayStrings(distanceAwayStrings);

    return destination;
  }

  @Override
  public RouteDestinationItem getRouteDestinationModelForRoute(StopGroupBean group, RouteBean route, List<StopResult> stops) {
    MobileWebRouteDestinationItem destination = new MobileWebRouteDestinationItem(group, stops);

    if(destination.getStops() == null)
      return destination;
      
    List<VehicleActivityStructure> journeyList = _realtimeService.getVehicleActivityForRoute(route.getId(), 
        null, false);

    // build map of stop IDs to list of distance strings
    Map<String, ArrayList<String>> stopIdToDistanceStringMap = new HashMap<String, ArrayList<String>>();      
    for(VehicleActivityStructure journey : journeyList) {
      MonitoredCallStructure monitoredCall = journey.getMonitoredVehicleJourney().getMonitoredCall();
      if(monitoredCall == null) 
        continue;

      // find latest update time across all realtime data
      Long thisLastUpdateTime = journey.getRecordedAtTime().getTime();
      if(thisLastUpdateTime != null && thisLastUpdateTime > _lastUpdateTime) {
        _lastUpdateTime = thisLastUpdateTime;
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

    return destination;       
  }
  
}