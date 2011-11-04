package org.onebusaway.nyc.presentation.impl.sort;

import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import java.util.Comparator;

public class SearchResultComparator implements Comparator<SearchResult> {

  @Override
  public int compare(SearchResult o1, SearchResult o2) {
    if ((o1 instanceof RouteResult) && (o2 instanceof StopResult))
      return -1;
    else if ((o1 instanceof StopResult)
        && (o2 instanceof RouteResult))
      return 1;
    else if ((o1 instanceof RouteResult)
        && (o2 instanceof RouteResult))
      return ((RouteResult) o1).getRouteId().compareTo(
          ((RouteResult) o2).getRouteId());
    else if ((o1 instanceof StopResult)
        && (o2 instanceof StopResult))
      return ((StopResult) o1).getName().compareTo(
          ((StopResult) o2).getName());
    else
      return 1;
  }
}