package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.Collections;
import java.util.List;

import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.springframework.stereotype.Component;

@Component
class VehicleTrackingManagementServiceImpl implements
    VehicleTrackingManagementService {

  private double _vehicleOffRouteDistanceThreshold = 500;

  private int _vehicleStalledTimeThreshold = 2 * 60;

  /****
   * {@link VehicleTrackingManagementService} Interface
   ****/

  @Override
  public double getVehicleOffRouteDistanceThreshold() {
    return _vehicleOffRouteDistanceThreshold;
  }

  @Override
  public int getVehicleStalledTimeThreshold() {
    return _vehicleStalledTimeThreshold;
  }

  @Override
  public void setVehicleOffRouteDistanceThreshold(
      double vehicleOffRouteDistanceThreshold) {
    _vehicleOffRouteDistanceThreshold = vehicleOffRouteDistanceThreshold;
  }

  @Override
  public void setVehicleStalledTimeThreshold(int vehicleStalledTimeThreshold) {
    _vehicleStalledTimeThreshold = vehicleStalledTimeThreshold;
  }

  @Override
  public void setVehicleStatus(String vehicleId, boolean status) {

  }

  @Override
  public List<NycVehicleStatusBean> getAllVehicleStatuses() {
    return Collections.emptyList();
  }

  @Override
  public NycVehicleStatusBean getVehicleStatusForVehicleId(String vehicleId) {
    return null;
  }
}
