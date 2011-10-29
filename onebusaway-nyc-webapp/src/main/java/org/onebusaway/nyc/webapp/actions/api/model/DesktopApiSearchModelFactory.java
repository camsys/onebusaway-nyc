package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.impl.DefaultSearchModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;

public class DesktopApiSearchModelFactory extends DefaultSearchModelFactory {
  
  @Override
  public RouteDestinationItem getRouteDestinationItemModel() {
    return new DesktopApiRouteDestinationItem();
  }
  
}