package org.onebusaway.nyc.sms.model;

import java.util.List;

import org.onebusaway.nyc.presentation.model.AvailableRoute;
import org.onebusaway.nyc.presentation.model.DistanceAway;
import org.onebusaway.nyc.presentation.model.search.SearchResult;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;

/**
 * Control logic for displaying search results within a sms character limit
 */
public class SmsDisplayer {

  /** search results from the nyc search service */
  private final List<SearchResult> searchResults;
  
  private static final int MAX_CHARS = 160;
  
  /** sms response returned back */
  private final StringBuilder response = new StringBuilder(SmsDisplayer.MAX_CHARS);

  public SmsDisplayer(List<SearchResult> searchResults) {
    this.searchResults = searchResults;
  }
  
  private void addToResponse(Object obj) {
    String s = obj.toString();
    int currentLength = response.length();
    if (currentLength + s.length() > SmsDisplayer.MAX_CHARS)
      return;
    response.append(s);
  }
  
  // prepare the different type of responses

  public void singleStopResponse() {
    if (searchResults.size() != 1) {
      addToResponse("Stop not found");
    } else {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResults.get(0);
      List<AvailableRoute> routesAvailable = stopSearchResult.getRoutesAvailable();
      for (AvailableRoute availableRoute : routesAvailable) {
        String routeId = availableRoute.getRouteId();
        List<DistanceAway> distanceAways = availableRoute.getDistanceAway();
        if (distanceAways.isEmpty()) {
          addToResponse(routeId + ": No upcoming arrivals\n");
        } else {
          for (DistanceAway distanceAway : distanceAways) {
            String presentableDistance = distanceAway.getPresentableDistance();
            addToResponse(routeId + ": " + presentableDistance + "\n");
          }
        }
      }
    }
  }

  public void noResultsResponse() {
    addToResponse("No stops found\n");
  }

  public void twoStopResponse() {
    for (SearchResult searchResult : searchResults) {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResult;
      String stopDirection = stopSearchResult.getStopDirection();
      addToResponse(stopDirection + ":\n");
      List<AvailableRoute> routesAvailable = stopSearchResult.getRoutesAvailable();
      for (AvailableRoute availableRoute : routesAvailable) {
        String routeId = availableRoute.getRouteId();
        List<DistanceAway> distanceAways = availableRoute.getDistanceAway();
        if (distanceAways.isEmpty()) {
          addToResponse(routeId + ": No upcoming arrivals\n");
        } else {
          for (DistanceAway distanceAway : distanceAways) {
            String presentableDistance = distanceAway.getPresentableDistance();
            addToResponse(routeId + ": " + presentableDistance + "\n");
          }
        }
      }
    }
  }

  public void manyStopResponse() {
    addToResponse("Send:\n");
    for (SearchResult searchResult : searchResults) {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResult;
      String stopIdNoAgency = stopSearchResult.getStopIdNoAgency();
      String stopDirection = stopSearchResult.getStopDirection();
      addToResponse(stopIdNoAgency + " for " + stopDirection + "\n");
    }
  }
  
  @Override
  public String toString() {
    return response.toString();
  }

}
