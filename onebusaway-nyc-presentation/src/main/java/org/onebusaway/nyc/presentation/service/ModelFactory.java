package org.onebusaway.nyc.presentation.service;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItemWithStops;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public interface ModelFactory {

  public StopSearchResult getSearchResultModelFor(StopBean stopBean, List<RouteItem> routesAvailable);

  public RouteSearchResult getSearchResultModelFor(RouteBean routeBean, 
      List<RouteDestinationItemWithStops> destinations);

  public RouteDestinationItem getRouteDestinationItemModelFor(String headsign, 
      String directionId, String polyline);

  public RouteDestinationItemWithStops 
    getRouteDestinationItemWithStopsModelFor(RouteDestinationItem destination, List<StopItem> stops);

  public StopItem getStopItemModelFor(StopBean stopBean);

  public RouteItem getRouteItemModelFor(RouteBean routeBean, List<RouteDestinationItem> destinations);

}