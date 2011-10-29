package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.service.SearchModelFactory;

import java.util.List;

public interface RouteSearchService {

  public void setModelFactory(SearchModelFactory factory);

  public List<RouteResult> resultsForLocation(Double latitude, Double longitude);

  public List<RouteResult> resultsForLocation(CoordinateBounds bounds);

  public List<RouteResult> resultsForQuery(String routeQuery);

  public RouteResult makeResultForRouteId(String routeId);

}