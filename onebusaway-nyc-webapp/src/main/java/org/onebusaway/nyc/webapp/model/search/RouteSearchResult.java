package org.onebusaway.nyc.webapp.model.search;

import java.util.List;

import org.onebusaway.nyc.webapp.model.StopItem;

/**
 * Data transfer object for route search results
 */
public class RouteSearchResult implements SearchResult {

  private final String routeId;
  private final String routeName;
  private final String routeDescription;
  private final String lastUpdate;
  private final String type;
  private final String tripHeadsign;
  private final String directionId;
  private final List<StopItem> stopItems;

  public RouteSearchResult(String routeId, String routeName,
      String routeDescription, String lastUpdate, String tripHeadsign, String directionId, List<StopItem> stopItems) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.routeDescription = routeDescription;
        this.lastUpdate = lastUpdate;
        this.tripHeadsign = tripHeadsign;
        this.directionId = directionId;
        this.stopItems = stopItems;
        this.type = "route";
  }

  public String getRouteId() {
    return routeId;
  }

  public String getName() {
    return routeName;
  }

  public String getDescription() {
    return routeDescription;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public String getType() {
    return type;
  }

  public String getTripHeadsign() {
    return tripHeadsign;
  }

  public String getDirectionId() {
    return directionId;
  }

  public List<StopItem> getStopItems() {
    return stopItems;
  }

}
