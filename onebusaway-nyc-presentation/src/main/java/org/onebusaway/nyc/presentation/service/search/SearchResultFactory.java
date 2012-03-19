package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;

import java.util.Set;

public interface SearchResultFactory {

  public SearchResult getRouteResult(RouteBean routeBean);

  public SearchResult getRouteResultForRegion(RouteBean routeBean);

  public SearchResult getStopResult(StopBean stopBean, Set<String> routeShortNameFilter);

  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<String> routeShortNameFilter);

}