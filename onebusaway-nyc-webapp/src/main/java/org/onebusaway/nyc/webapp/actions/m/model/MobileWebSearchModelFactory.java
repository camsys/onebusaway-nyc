package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.impl.DefaultSearchModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.StopResult;

public class MobileWebSearchModelFactory extends DefaultSearchModelFactory {

  public StopResult getStopSearchResultModel() {
    return new MobileWebStopResult();
  }

  public RouteDestinationItem getRouteDestinationItemModel() {
    return new MobileWebRouteDestinationItem();
  }

}