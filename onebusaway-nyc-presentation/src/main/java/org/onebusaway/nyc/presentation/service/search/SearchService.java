package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.transit_data.model.RouteBean;

import java.util.Set;

public interface SearchService {

  public SearchResultCollection getSearchResults(String query, SearchResultFactory resultFactory);

  public SearchResultCollection findRoutesStoppingNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory);

  public SearchResultCollection findRoutesStoppingWithinRegion(CoordinateBounds bounds, SearchResultFactory resultFactory);

  public SearchResultCollection findStopsNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory, Set<RouteBean> routeFilter);

}