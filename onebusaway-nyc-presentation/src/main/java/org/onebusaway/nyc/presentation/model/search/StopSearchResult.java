package org.onebusaway.nyc.presentation.model.search;

import java.util.List;

import org.onebusaway.nyc.presentation.model.AvailableRoute;

/**
 * Data transfer object for route search results
 */
public class StopSearchResult implements SearchResult {

  private final String stopId;
  private final String name;
  private final List<Double> latlng;
  private final String type;
  private final List<AvailableRoute> availableRoutes;

  public StopSearchResult(String stopId, String name, List<Double> latlng, List<AvailableRoute> availableRoutes) {
    this.stopId = stopId;
    this.name = name;
    this.latlng = latlng;
    this.availableRoutes = availableRoutes;
    this.type = "stop";
  }

  public String getStopId() {
    return stopId;
  }

  public String getName() {
    return name;
  }

  public List<Double> getLatlng() {
    return latlng;
  }

  public String getType() {
    return type;
  }

  public List<AvailableRoute> getRoutesAvailable() {
    return availableRoutes;
  }
  
}
