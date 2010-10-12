package org.onebusaway.nyc.webapp.model;

import org.onebusaway.nyc.presentation.model.DistanceAway;

/**
 * Data transfer object for a vehicle's next stop
 */
public class NextStop {

  private final String vehicleId;
  private final String stopId;
  private final String name;
  private final DistanceAway distanceAway;
  private final String lastUpdate;

  public NextStop(String vehicleId, String stopId, String name, DistanceAway distanceAway, String lastUpdate) {
    this.vehicleId = vehicleId;
    this.stopId = stopId;
    this.name = name;
    this.distanceAway = distanceAway;
    this.lastUpdate = lastUpdate;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public String getStopId() {
    return stopId;
  }

  public String getName() {
    return name;
  }

  public DistanceAway getDistanceAway() {
    return distanceAway;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }
}
