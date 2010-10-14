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
  
  /** sms response returned back */
  private final StringBuilder response = new StringBuilder(160);

  public SmsDisplayer(List<SearchResult> searchResults) {
    this.searchResults = searchResults;
  }
  
  // prepare the different type of responses

  public void singleStopResponse() {
    if (searchResults.size() != 1) {
      response.append("Stop not found");
    } else {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResults.get(0);
      List<AvailableRoute> routesAvailable = stopSearchResult.getRoutesAvailable();
      for (AvailableRoute availableRoute : routesAvailable) {
        String routeId = availableRoute.getRouteId();
        List<DistanceAway> distanceAways = availableRoute.getDistanceAway();
        if (distanceAways.isEmpty()) {
          response.append(routeId + ": No upcoming arrivals\n");
        } else {
          for (DistanceAway distanceAway : distanceAways) {
            String presentableDistance = distanceAway.getPresentableDistance();
            response.append(routeId + ": " + presentableDistance + "\n");
          }
        }
      }
    }
  }

  public void noResultsResponse() {
    response.append("No stops found");
  }

  public void twoStopResponse() {
    for (SearchResult searchResult : searchResults) {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResult;
      String stopDirection = stopSearchResult.getStopDirection();
      response.append(stopDirection + ":\n");
      List<AvailableRoute> routesAvailable = stopSearchResult.getRoutesAvailable();
      for (AvailableRoute availableRoute : routesAvailable) {
        String routeId = availableRoute.getRouteId();
        List<DistanceAway> distanceAways = availableRoute.getDistanceAway();
        if (distanceAways.isEmpty()) {
          response.append(routeId + ": No upcoming arrivals\n");
        } else {
          for (DistanceAway distanceAway : distanceAways) {
            String presentableDistance = distanceAway.getPresentableDistance();
            response.append(routeId + ": " + presentableDistance + "\n");
          }
        }
      }
    }
  }

  public void manyStopResponse() {
    response.append("Send:\n");
    for (SearchResult searchResult : searchResults) {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResult;
      String stopIdNoAgency = stopSearchResult.getStopIdNoAgency();
      String stopDirection = stopSearchResult.getStopDirection();
      response.append(stopIdNoAgency + " for " + stopDirection + "\n");
    }
  }
  
  @Override
  public String toString() {
    return response.toString();
  }

}
