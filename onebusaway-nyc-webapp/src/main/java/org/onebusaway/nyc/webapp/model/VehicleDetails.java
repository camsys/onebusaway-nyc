package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for the details on a vehicle
 */
public class VehicleDetails {

  private final String vehicleId;
  private final String description;
  private final String lastUpdate;
  private final String routeId;
  private final List<NextStop> nextStops;

  public VehicleDetails(String vehicleId, String description, String lastUpdate, String routeId, List<NextStop> nextStops) {
    this.vehicleId = vehicleId;
    this.description = description;
    this.lastUpdate = lastUpdate;
    this.routeId = routeId;
    this.nextStops = nextStops;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public String getDescription() {
    return description;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public String getRouteId() {
    return routeId;
  }

  public List<NextStop> getNextStops() {
    return nextStops;
  }

}
