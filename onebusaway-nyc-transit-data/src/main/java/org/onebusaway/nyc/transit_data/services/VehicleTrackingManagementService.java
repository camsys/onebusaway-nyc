/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode);
  
  public boolean isUnknownDestinationSignCode(String destinationSignCode);
  
  void setVehicleStalledTimeThreshold(int vehicleStalledTimeThreshold);
}
