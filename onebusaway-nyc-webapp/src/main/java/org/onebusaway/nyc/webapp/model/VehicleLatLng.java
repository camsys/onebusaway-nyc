package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for a vehicle and its latlng
 */
public class VehicleLatLng {

  private final String vehicleId;
  private final List<Double> latLngs;

  public VehicleLatLng(String vehicleId, List<Double> latLngs) {
    this.vehicleId = vehicleId;
    this.latLngs = latLngs;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public List<Double> getLatLng() {
    return latLngs;
  }

}
