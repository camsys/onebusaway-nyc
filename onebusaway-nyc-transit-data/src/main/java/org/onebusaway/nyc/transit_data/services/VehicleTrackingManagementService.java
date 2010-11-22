package org.onebusaway.nyc.transit_data.services;

import java.util.List;

import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;

public interface VehicleTrackingManagementService {
  
  public String getDefaultAgencyId();

  public void setVehicleStatus(String vehicleId, boolean status);
  
  public void resetVehicleTrackingForVehicleId(String vehicleId);

  public List<NycVehicleStatusBean> getAllVehicleStatuses();

  public NycVehicleStatusBean getVehicleStatusForVehicleId(String vehicleId);

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

  void setVehicleOffRouteDistanceThreshold(
      double vehicleOffRouteDistanceThreshold);

  void setVehicleStalledTimeThreshold(int vehicleStalledTimeThreshold);
}
