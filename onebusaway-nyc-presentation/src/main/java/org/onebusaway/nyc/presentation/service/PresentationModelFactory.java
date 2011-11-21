package org.onebusaway.nyc.presentation.service;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;

import java.util.List;

public abstract interface PresentationModelFactory {
  
  // stop as top-level object
  public StopResult getStopModel(StopBean stopBean, List<RouteResult> routesAvailable);

  // stop on route
  public StopResult getStopModelForRoute(StopBean stopBean, RouteBean route);

  // route as top-level object
  public RouteResult getRouteModel(RouteBean routeBean, List<RouteDestinationItem> destinations);

  // route at stop
  public RouteResult getRouteModelForStop(RouteBean routeBean, List<RouteDestinationItem> destinations);

  // route direction at route (route being top level object)
  public RouteDestinationItem getRouteDestinationModelForRoute(StopGroupBean group, RouteBean route, List<StopResult> stops);

  // route available at stop (stop being top level object)
  public RouteDestinationItem getRouteDestinationModelForStop(StopGroupBean group, RouteBean route, StopBean stop);

}