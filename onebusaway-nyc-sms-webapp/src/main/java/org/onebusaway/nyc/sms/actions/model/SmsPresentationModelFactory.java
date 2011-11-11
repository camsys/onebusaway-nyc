package org.onebusaway.nyc.sms.actions.model;

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

import java.util.ArrayList;
import java.util.List;

public class SmsPresentationModelFactory extends DefaultPresentationModelFactory {

  private RealtimeService _realtimeService;
  
  public SmsPresentationModelFactory(RealtimeService realtimeService) {
    _realtimeService = realtimeService;
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
      if(monitoredCall == null) 
        continue;

      SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
      SiriDistanceExtension distanceExtension = wrapper.getDistances();
      distanceAwayStrings.add(distanceExtension.getPresentableDistance());
    }

    item.setDistanceAwayStrings(distanceAwayStrings);

    return item;
  }

}