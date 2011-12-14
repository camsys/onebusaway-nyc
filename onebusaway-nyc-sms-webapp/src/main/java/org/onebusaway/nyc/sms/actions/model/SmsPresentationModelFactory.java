package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

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
    List<NaturalLanguageStringBean> serviceAlertSummaries = new ArrayList<NaturalLanguageStringBean>();

    List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRoute(route.getId());
    for(ServiceAlertBean serviceAlertBean : serviceAlertBeans) {
      for(NaturalLanguageStringBean summary : serviceAlertBean.getSummaries()) {
        serviceAlertSummaries.add(summary);
      }
    }
    
    item.setServiceAlerts(serviceAlertSummaries);

    return item;
  }
  
  @Override  
  public RouteDestinationItem getRouteDestinationModelForStop(StopGroupBean group, RouteBean route, StopBean stop) {
    SmsRouteDestinationItem item = new SmsRouteDestinationItem(group, null);

    // service alerts
    List<NaturalLanguageStringBean> serviceAlertSummaries = new ArrayList<NaturalLanguageStringBean>();

    List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRoute(route.getId());
    for(ServiceAlertBean serviceAlertBean : serviceAlertBeans) {      
      for(NaturalLanguageStringBean summary : serviceAlertBean.getSummaries()) {
        serviceAlertSummaries.add(summary);
      }
    }
    
    item.setServiceAlerts(serviceAlertSummaries);
    
    // stop visits
    List<MonitoredStopVisitStructure> visits = _realtimeService.getMonitoredStopVisitsForStop(stop.getId(), 0);

    List<String> distanceAwayStrings = new ArrayList<String>();
    for(MonitoredStopVisitStructure visit : visits) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      if(!route.getId().equals(routeId))
        continue;

      // no next stop = detoured
      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
      if(monitoredCall == null)
        continue;

      Long thisLastUpdateTime = visit.getRecordedAtTime().getTime();
      distanceAwayStrings.add(getPresentableDistance(visit.getMonitoredVehicleJourney(), thisLastUpdateTime));
    }

    item.setDistanceAwayStrings(distanceAwayStrings);

    return item;
  }
  
  private String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime) {
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