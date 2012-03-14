package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;

import java.util.List;

public interface SearchService {

  public SearchResultCollection getSearchResults(String query, SearchResultFactory resultFactory);

  public List<SearchResult> getRouteResultsStoppingNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory);

  public List<SearchResult> getRouteResultsStoppingWithinRegion(CoordinateBounds bounds, SearchResultFactory resultFactory);

  public List<SearchResult> getStopResultsNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory);

}