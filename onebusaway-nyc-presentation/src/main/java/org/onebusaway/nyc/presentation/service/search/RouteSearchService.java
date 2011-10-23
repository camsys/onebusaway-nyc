package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.service.ModelFactory;

import java.util.List;

public interface RouteSearchService {

  public void setModelFactory(ModelFactory factory);

  public List<RouteItem> itemsForLocation(Double latitude, Double longitude);

  public List<RouteItem> itemsForLocation(CoordinateBounds bounds);

  public List<RouteSearchResult> makeResultFor(String route);
  
}