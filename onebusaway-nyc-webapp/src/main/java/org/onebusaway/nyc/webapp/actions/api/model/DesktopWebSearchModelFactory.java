package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public class DesktopWebSearchModelFactory extends DefaultPresentationModelFactory {

  private RouteSearchService _routeSearchService;
  
  public DesktopWebSearchModelFactory(RouteSearchService routeSearchService) {
    _routeSearchService = routeSearchService;
  }
  
  @Override
  public StopResult getStopModel(StopBean stopBean, List<RouteResult> routesAvailable) {
    DesktopWebStopResult item = new DesktopWebStopResult(stopBean, routesAvailable);
    
    item.setNearbyRoutes(_routeSearchService.resultsForLocation(stopBean.getLat(), stopBean.getLon()));
    
    return item;
  }
}