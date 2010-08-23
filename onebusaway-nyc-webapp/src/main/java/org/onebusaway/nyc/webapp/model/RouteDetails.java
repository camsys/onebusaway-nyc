package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for the details of a route, including its geometry
 */
public class RouteDetails {

  private final String routeId;
  private final List<List<Double>> routeLatLngs;
  private final String name;
  private final String description;
  private final String lastUpdated;

  public RouteDetails(String routeId, String name, String description, String lastUpdated, List<List<Double>> routeLatLngs) {
    this.routeId = routeId;
    this.name = name;
    this.description = description;
    this.lastUpdated = lastUpdated;
    this.routeLatLngs = routeLatLngs;
  }

  public String getRouteId() {
    return routeId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  public List<List<Double>> getPolyLine() {
    return routeLatLngs;
  }

}
