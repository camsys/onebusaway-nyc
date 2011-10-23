package org.onebusaway.nyc.presentation.impl.sort;

import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import java.util.Comparator;

public class SearchResultComparator implements Comparator<SearchResult> {

  @Override
  public int compare(SearchResult o1, SearchResult o2) {
    if ((o1 instanceof RouteSearchResult) && (o2 instanceof StopSearchResult))
      return -1;
    else if ((o1 instanceof StopSearchResult)
        && (o2 instanceof RouteSearchResult))
      return 1;
    else if ((o1 instanceof RouteSearchResult)
        && (o2 instanceof RouteSearchResult))
      return ((RouteSearchResult) o1).getRouteId().compareTo(
          ((RouteSearchResult) o2).getRouteId());
    else if ((o1 instanceof StopSearchResult)
        && (o2 instanceof StopSearchResult))
      return ((StopSearchResult) o1).getName().compareTo(
          ((StopSearchResult) o2).getName());
    else
      return 1;
  }
}