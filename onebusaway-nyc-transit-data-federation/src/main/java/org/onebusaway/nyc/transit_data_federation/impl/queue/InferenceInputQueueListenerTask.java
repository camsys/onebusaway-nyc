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
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

public class InferenceInputQueueListenerTask extends InferenceQueueListenerTask {

  @Autowired
  private VehicleLocationListener _vehicleLocationListener;

  @Autowired
  private PredictionIntegrationService _predictionIntegrationService;

  @Autowired
  private VehicleTrackingManagementService _vehicleTrackingManagementService;

  public String getQueueDisplayName() {
    return "InferenceInputQueueListenerTask";
  }
  
  @Override
  protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents) {
    VehicleLocationRecord vlr = new VehicleLocationRecord();
    vlr.setVehicleId(AgencyAndIdLibrary.convertFromString(inferredResult.getVehicleId()));
    vlr.setTimeOfRecord(inferredResult.getRecordTimestamp());
    vlr.setTimeOfLocationUpdate(inferredResult.getRecordTimestamp());
    vlr.setBlockId(AgencyAndIdLibrary.convertFromString(inferredResult.getBlockId()));
    vlr.setTripId(AgencyAndIdLibrary.convertFromString(inferredResult.getTripId()));
    vlr.setServiceDate(inferredResult.getServiceDate());
    vlr.setDistanceAlongBlock(inferredResult.getDistanceAlongBlock());
    vlr.setCurrentLocationLat(inferredResult.getInferredLatitude());
    vlr.setCurrentLocationLon(inferredResult.getInferredLongitude());
    vlr.setPhase(EVehiclePhase.valueOf(inferredResult.getPhase()));
    vlr.setStatus(inferredResult.getStatus());

    _vehicleLocationListener.handleVehicleLocationRecord(vlr);
    _predictionIntegrationService.updatePredictionsForVehicle(vlr.getVehicleId());
    _vehicleTrackingManagementService.handleRecord(inferredResult);
  }

}
