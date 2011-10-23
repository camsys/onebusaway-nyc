package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.impl.DefaultModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public class MobileWebModelFactory extends DefaultModelFactory {

  @Override
  public RouteItem getRouteItemModelFor(RouteBean routeBean, List<RouteDestinationItem> destinations) {
    return new MobileWebRouteItem(routeBean, destinations);    
  }

  @Override
  public RouteDestinationItem getRouteDestinationItemModelFor(String headsign, 
      String directionId, String polyline) {
    return new MobileWebRouteDestinationItem(headsign, directionId, polyline);
  }

  @Override
  public StopItem getStopItemModelFor(StopBean stopBean) {
    return new MobileWebStopItem(stopBean);    
  }

}