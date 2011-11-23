package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public class DesktopWebPresentationModelFactory extends DefaultPresentationModelFactory {

  private RouteSearchService _routeSearchService;

  private RealtimeService _realtimeService;

  public DesktopWebPresentationModelFactory(RealtimeService realtimeService, RouteSearchService routeSearchService) {
    _realtimeService = realtimeService;
    _routeSearchService = routeSearchService;
  }
  
  @Override
  public RouteResult getRouteModel(RouteBean routeBean, List<RouteDestinationItem> destinations) {
    DesktopWebRouteResult routeResult = new DesktopWebRouteResult(routeBean, destinations);

    routeResult.setServiceAlerts(_realtimeService.getServiceAlertsForRoute(routeBean.getId()));
    
    return routeResult;
  }
  
  @Override
  public StopResult getStopModel(StopBean stopBean, List<RouteResult> routesAvailable) {
    DesktopWebStopResult item = new DesktopWebStopResult(stopBean, routesAvailable);
    
    item.setNearbyRoutes(_routeSearchService.resultsForLocation(stopBean.getLat(), stopBean.getLon()));
    
    return item;
  }
}