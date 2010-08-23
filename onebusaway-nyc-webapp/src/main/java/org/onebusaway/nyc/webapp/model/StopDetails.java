package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for the details on a stop
 */
public class StopDetails {

  private final String stopId;
  private final String stopName;
  private final List<Double> latLng;
  private final String lastUpdate;
  private final List<AvailableRoute> availableRoutes;

  public StopDetails(String stopId, String stopName, List<Double> latLng, String lastUpdate, List<AvailableRoute> availableRoutes) {
    this.stopId = stopId;
    this.stopName = stopName;
    this.latLng = latLng;
    this.lastUpdate = lastUpdate;
    this.availableRoutes = availableRoutes;
  }

  public String getStopId() {
    return stopId;
  }

  public String getName() {
    return stopName;
  }

  public List<Double> getLatLng() {
    return latLng;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public List<AvailableRoute> getRoutesAvailable() {
    return availableRoutes;
  }

}
