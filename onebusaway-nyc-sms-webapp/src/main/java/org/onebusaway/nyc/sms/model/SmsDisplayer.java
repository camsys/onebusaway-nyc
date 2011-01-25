/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.sms.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.onebusaway.nyc.presentation.model.RouteItem;
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
      List<RouteItem> routesAvailable = stopSearchResult.getRoutesAvailable();
      if (routesAvailable.isEmpty()) {
        addToResponse("No upcoming service at this stop\n");
      } else {
        for (RouteItem availableRoute : routesAvailable) {
          String routeId = availableRoute.getRouteId();
          List<DistanceAway> distanceAways = availableRoute.getDistanceAways();
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
  }

  public void noResultsResponse() {
    addToResponse("No stops found\n");
  }
  
  private List<RouteDistanceAway> getRouteDistanceAways(List<RouteItem> routes) {
    // flatten out the available route distanceaway structure for easier iteration
    List<RouteDistanceAway> result = new ArrayList<RouteDistanceAway>();
    for (RouteItem availableRoute : routes) {
      String routeId = availableRoute.getRouteId();
      for (DistanceAway distanceAway : availableRoute.getDistanceAways()) {
        RouteDistanceAway routeDistanceAway = new RouteDistanceAway(routeId, distanceAway);
        result.add(routeDistanceAway);
      }
    }
    return result;
  }
  
  private StringBuilder getInitialTwoStopResponse(StopSearchResult stopSearchResult) {
    // helper function to help eliminate duplicate code
    StringBuilder result = new StringBuilder(SmsDisplayer.MAX_CHARS/2);
    String stopDirection = stopSearchResult.getStopDirection();
    String stopIdNoAgency = stopSearchResult.getStopIdNoAgency();
    result.append(stopDirection + "-bound (" + stopIdNoAgency + "):\n");
    return result;
  }
  
  private String getNoUpcomingArrivalsString(List<RouteItem> routes) {
    StringBuilder result = new StringBuilder();
    for (RouteItem availableRoute : routes) {
      String routeId = availableRoute.getRouteId();
      result.append(routeId + ": No upcoming arrivals\n");
    }
    return result.toString();
  }

  public void twoStopResponse() {
    // when limiting the sms response for two responses, we need to limit the responses in pairs
    StopSearchResult stopSearchResult1 = (StopSearchResult) searchResults.get(0);
    StopSearchResult stopSearchResult2 = (StopSearchResult) searchResults.get(1);
    StringBuilder stopResponse1 = getInitialTwoStopResponse(stopSearchResult1);
    StringBuilder stopResponse2 = getInitialTwoStopResponse(stopSearchResult2);
    List<RouteItem> routesAvailable1 = stopSearchResult1.getRoutesAvailable();
    List<RouteItem> routesAvailable2 = stopSearchResult2.getRoutesAvailable();
    List<RouteDistanceAway> rdaList1 = getRouteDistanceAways(routesAvailable1);
    List<RouteDistanceAway> rdaList2 = getRouteDistanceAways(routesAvailable2);
    Iterator<RouteDistanceAway> it1 = rdaList1.iterator();
    Iterator<RouteDistanceAway> it2 = rdaList2.iterator();
    
    // special check for no upcoming arrivals for either stop
    if (rdaList1.isEmpty())
      stopResponse1.append(getNoUpcomingArrivalsString(routesAvailable1));
    if (rdaList2.isEmpty())
      stopResponse2.append(getNoUpcomingArrivalsString(routesAvailable2));

    while (true) {
      if (!it1.hasNext() && !it2.hasNext())
        break;
      String s1 = "";
      String s2 = "";
      if (it1.hasNext()) {
        RouteDistanceAway rda1 = it1.next();
        s1 = rda1.toString();
      }
      if (it2.hasNext()) {
        RouteDistanceAway rda2 = it2.next();
        s2 = rda2.toString();
      }
      if (s1.length() + s2.length() + stopResponse1.length() + stopResponse2.length() <= SmsDisplayer.MAX_CHARS) {
        stopResponse1.append(s1);
        stopResponse2.append(s2);
      }
    }
    addToResponse(stopResponse1);
    addToResponse(stopResponse2);
  }

  public void manyStopResponse() {
    addToResponse("Send:\n");
    for (SearchResult searchResult : searchResults) {
      StopSearchResult stopSearchResult = (StopSearchResult) searchResult;
      String stopIdNoAgency = stopSearchResult.getStopIdNoAgency();
      String stopDirection = stopSearchResult.getStopDirection();
      addToResponse(stopIdNoAgency + " for " + stopDirection + "-bound\n");
    }
  }
  
  @Override
  public String toString() {
    return response.toString();
  }
  
  private static class RouteDistanceAway {
    private final String routeId;
    private final DistanceAway distanceAway;

    // create a structure that associates a route id and a distance away
    public RouteDistanceAway(String routeId, DistanceAway distanceAway) {
      this.routeId = routeId;
      this.distanceAway = distanceAway;
    }
    
    @Override
    public String toString() {
      return routeId + ": " + distanceAway.getPresentableDistance() + "\n";
    }
  }

}
