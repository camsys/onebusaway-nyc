package org.onebusaway.nyc.webapp.model.search;

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

  public RouteSearchResult(String routeId, String routeName,
      String routeDescription, String lastUpdate, String tripHeadsign, String directionId) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.routeDescription = routeDescription;
        this.lastUpdate = lastUpdate;
        this.tripHeadsign = tripHeadsign;
        this.directionId = directionId;
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

}
