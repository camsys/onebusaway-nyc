package org.onebusaway.nyc.presentation.impl;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItemWithStops;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.ModelFactory;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public class DefaultModelFactory implements ModelFactory {
  
  @Override
  public StopSearchResult getSearchResultModelFor(StopBean stopBean, List<RouteItem> routesAvailable) {
    return new StopSearchResult(stopBean, routesAvailable);
  }

  @Override
  public RouteSearchResult getSearchResultModelFor(RouteBean routeBean, 
      List<RouteDestinationItemWithStops> destinations) {
    return new RouteSearchResult(routeBean, destinations);
  }

  @Override
  public StopItem getStopItemModelFor(StopBean stopBean) {
    return new StopItem(stopBean);
  }

  @Override
  public RouteItem getRouteItemModelFor(RouteBean routeBean, List<RouteDestinationItem> destinations) {
    return new RouteItem(routeBean, destinations);    
  }

  @Override
  public RouteDestinationItem getRouteDestinationItemModelFor(String headsign, 
      String directionId, String polyline) {
    return new RouteDestinationItem(headsign, directionId, polyline);
  }

  @Override
  public RouteDestinationItemWithStops getRouteDestinationItemWithStopsModelFor(
      RouteDestinationItem destination, List<StopItem> stops) {
    return new RouteDestinationItemWithStops(destination, stops);
  }

}