package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.impl.realtime.siri.model.SiriDistanceExtension;
import org.onebusaway.nyc.presentation.impl.realtime.siri.model.SiriExtensionWrapper;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;

import java.util.ArrayList;
import java.util.List;

public class SmsPresentationModelFactory extends DefaultPresentationModelFactory {

  private RealtimeService _realtimeService;

  private ConfigurationService _configurationService;

  public SmsPresentationModelFactory(RealtimeService realtimeService, ConfigurationService configurationService) {
    _realtimeService = realtimeService;
    _configurationService = configurationService;
  }

  @Override
  public RouteDestinationItem getRouteDestinationModelForRoute(StopGroupBean group, RouteBean route, List<StopResult> stops) {
    SmsRouteDestinationItem item = new SmsRouteDestinationItem(group, stops);

    // service alerts
    List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForRouteAndDirection(route.getId(), group.getId());
    item.setServiceAlerts(serviceAlerts);

    return item;
  }
  
  @Override  
  public RouteDestinationItem getRouteDestinationModelForStop(StopGroupBean group, RouteBean route, StopBean stop) {
    SmsRouteDestinationItem item = new SmsRouteDestinationItem(group, null);

    // service alerts
    List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForStop(stop.getId());
    item.setServiceAlerts(serviceAlerts);

    // stop visits
    List<MonitoredStopVisitStructure> visits = _realtimeService.getMonitoredStopVisitsForStop(stop.getId(), false);

    List<String> distanceAwayStrings = new ArrayList<String>();
    for(MonitoredStopVisitStructure visit : visits) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      if(!route.getId().equals(routeId) || !group.getId().equals(directionId))
        continue;

      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();

      // no next stop = detoured
      if(monitoredCall == null)
        continue;

      Long thisLastUpdateTime = visit.getRecordedAtTime().getTime();

      distanceAwayStrings.add(getPresentableDistance(visit.getMonitoredVehicleJourney(), thisLastUpdateTime));
    }

    item.setDistanceAwayStrings(distanceAwayStrings);

    return item;
  }
  
  public String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime) {
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
    SiriDistanceExtension distanceExtension = wrapper.getDistances();    
        
    String message = "";
    String distance = distanceExtension.getPresentableDistance();

    NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
    if(progressStatus != null && progressStatus.getValue().equals("layover")) {
      message += "@terminal";
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
      return distance + " (" + message + ")";
    else
      return distance;
  }
 
}