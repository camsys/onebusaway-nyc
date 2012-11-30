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

import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;

/**
 * Service to receive NycVehicleManagementStatusBeans and to interact with the inference engine
 * infrastructure from the front-end (admin) console.
 * 
 * @author jmaki
 *
 */
public interface VehicleTrackingManagementService {
      
  public List<NycVehicleManagementStatusBean> getAllVehicleManagementStatusBeans();

  public NycVehicleManagementStatusBean getVehicleManagementStatusBeanForVehicleId(String vehicleId);

  public void handleRecord(NycQueuedInferredLocationBean record);

  /**
   * DSC methods used in the vehicles admin screen and the integration tests
   */
  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode);
  
  public boolean isMissingDestinationSignCode(String destinationSignCode);
  
  public boolean isUnknownDestinationSignCode(String destinationSignCode);
}
