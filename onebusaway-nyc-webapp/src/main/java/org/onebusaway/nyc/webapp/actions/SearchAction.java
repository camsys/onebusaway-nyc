package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.search.RouteSearchResult;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.nyc.webapp.model.search.StopSearchResult;

/**
 * Handles requests for a generic search. Can return route/stop specific results.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  private final List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Override
  public String execute() {
    searchResults.add(makeRouteSearchResult("M14D", "14th Street Crosstown via Avenue D"));
    searchResults.add(makeRouteSearchResult("M14A", "14th Street Crosstown via Avenue A"));
    searchResults.add(makeStopSearchResult("S000001", "Mulberry and Canal"));
    searchResults.add(makeStopSearchResult("S000002", "Allen and Delancey"));
    return SUCCESS;
  }  

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }
  
  // FIXME this just returns stubbed out data
  private RouteSearchResult makeRouteSearchResult(String routeId, String routeDescription) {
    return new RouteSearchResult(routeId, routeId, routeDescription, "1 minute ago");
  }

  private SearchResult makeStopSearchResult(String stopId, String stopName) {
    List<Double> latlng = stopId.equals("S000001")
      ? makeLatLng(40.717078345319955, -73.9985418201294)
      : makeLatLng(40.71912753071832, -73.99034498937989);
    return new StopSearchResult(stopId, stopName, latlng, makeAvailableRoutes());
  }
  
}
