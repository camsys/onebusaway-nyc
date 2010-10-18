package org.onebusaway.nyc.vehicle_tracking.services;

public interface VehicleTrackingConfigurationService {

  /**
   * A vehicle actively serving a block that is more than the specified distance
   * threshold from the block path will be considered off route.
   * 
   * @return the distance threshold, in meters
   */
  public double getVehicleOffRouteDistanceThreshold();

  /**
   * A vehicle actively serving a block that hasn't moved in the specified
   * amount of time will be considered stalled.
   * 
   * @return the time threshold, in seconds
   */
  public int getVehicleStalledTimeThreshold();
}
