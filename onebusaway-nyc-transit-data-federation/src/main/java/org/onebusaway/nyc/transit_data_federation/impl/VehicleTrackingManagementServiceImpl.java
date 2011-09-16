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
package org.onebusaway.nyc.transit_data_federation.impl;

import java.util.List;

import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.transit_data_federation.model.VehicleLocationManagementRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class VehicleTrackingManagementServiceImpl implements VehicleTrackingManagementService {

  private static Logger _log = LoggerFactory.getLogger(VehicleTrackingManagementServiceImpl.class);

  /****
   * {@link VehicleTrackingManagementService} Interface
   ****/
  @Override
  public void setVehicleStatus(String vehicleId, boolean status) {
  }

  @Override
  public void resetVehicleTrackingForVehicleId(String vehicleId) {
  }

  @Override
  public List<NycVehicleStatusBean> getAllVehicleStatuses() {
	  return null;
  }

  @Override
  public NycVehicleStatusBean getVehicleStatusForVehicleId(String vehicleId) {
	  return null;
  }
  
  public void updateInternalStateWithRecord(NycQueuedInferredLocationBean record) {
	  // FIXME
  }
  
  /****
   * Private Methods
   ****/
  private NycVehicleStatusBean getManagementRecordAsStatus(VehicleLocationManagementRecord record) {

	if (record == null)
      return null;
    
    NycVehicleStatusBean bean = new NycVehicleStatusBean();
    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
    bean.setEnabled(record.isEnabled());
    bean.setLastUpdateTime(record.getLastUpdateTime());
    bean.setLastGpsTime(record.getLastGpsUpdateTime());
    bean.setLastGpsLat(record.getLastGpsLat());
    bean.setLastGpsLon(record.getLastGpsLon());
    bean.setMostRecentObservedDestinationSignCode(record.getMostRecentObservedDestinationSignCode());
    bean.setInferredDestinationSignCode(record.getInferredDestinationSignCode());

    return bean;
  }

}
