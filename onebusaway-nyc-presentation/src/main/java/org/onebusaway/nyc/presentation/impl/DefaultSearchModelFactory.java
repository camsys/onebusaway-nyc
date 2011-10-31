package org.onebusaway.nyc.presentation.impl;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.SearchModelFactory;

public class DefaultSearchModelFactory implements SearchModelFactory {
  
  public StopResult getStopSearchResultModel() {
    return new StopResult();
  }

  public RouteResult getRouteSearchResultModel() {
    return new RouteResult();
  }

  public RouteDestinationItem getRouteDestinationItemModel() {
    return new RouteDestinationItem();
  }

}