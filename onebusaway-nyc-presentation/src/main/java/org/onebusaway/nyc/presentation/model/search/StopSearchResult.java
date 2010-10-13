package org.onebusaway.nyc.presentation.model.search;

import java.util.List;

import org.onebusaway.nyc.presentation.impl.WebappIdParser;
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
  private final String stopDirection;

  public StopSearchResult(String stopId, String name, List<Double> latlng, String stopDirection, List<AvailableRoute> availableRoutes) {
    this.stopId = stopId;
    this.name = name;
    this.latlng = latlng;
    this.availableRoutes = availableRoutes;
    this.stopDirection = stopDirection;
    this.type = "stop";
  }

  public String getStopId() {
    return stopId;
  }
  
  public String getStopIdNoAgency() {
    WebappIdParser webappIdParser = new WebappIdParser();
    return webappIdParser.parseIdWithoutAgency(stopId);
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

  public String getStopDirection() {
    return stopDirection;
  }

  public List<AvailableRoute> getRoutesAvailable() {
    return availableRoutes;
  }
  
}
