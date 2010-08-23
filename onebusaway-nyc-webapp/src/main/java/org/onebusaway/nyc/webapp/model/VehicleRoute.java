package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for the vehicles belonging to a particular route
 */
public class VehicleRoute {

  private final String routeId;
  private final List<VehicleLatLng> vehicleLatLngs;

  public VehicleRoute(String routeId, List<VehicleLatLng> vehicleLatLngs) {
    this.routeId = routeId;
    this.vehicleLatLngs = vehicleLatLngs;
  }

  public String getRouteId() {
    return routeId;
  }

  public List<VehicleLatLng> getVehicles() {
    return vehicleLatLngs;
  }

}
