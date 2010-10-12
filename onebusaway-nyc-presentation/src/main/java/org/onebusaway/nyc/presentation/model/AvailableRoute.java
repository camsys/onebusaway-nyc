package org.onebusaway.nyc.presentation.model;

import java.util.List;

/**
 * Data transfer object for routes available to a stop,
 * and how far the next few vehicles are.
 */
public class AvailableRoute {

  private final String routeId;
  private final String routeDescription;
  private final List<DistanceAway> distanceAways;

  public AvailableRoute(String routeId, String routeDescription,
      List<DistanceAway> distanceAways) {
        this.routeId = routeId;
        this.routeDescription = routeDescription;
        this.distanceAways = distanceAways;
  }

  public String getRouteId() {
    return routeId;
  }

  public String getDescription() {
    return routeDescription;
  }

  public List<DistanceAway> getDistanceAway() {
    return distanceAways;
  }

}
