package org.onebusaway.nyc.presentation.service;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;

public abstract interface SearchModelFactory {

  public StopResult getStopSearchResultModel();

  public RouteResult getRouteSearchResultModel();

  public RouteDestinationItem getRouteDestinationItemModel();
  
}