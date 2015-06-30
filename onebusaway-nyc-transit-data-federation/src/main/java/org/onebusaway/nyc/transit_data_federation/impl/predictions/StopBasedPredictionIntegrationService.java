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
package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionGenerationService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A component to update predictions for a vehicle when an update is received from the vehicle.
 * The workflow is that the system updates the TDS with the inferred result, queries the PredictionGenerationService,
 * and then updates the TDS with those predicted results.
 * 
 * This service exists as a way to synchronize and match those two results across what might be two separate systems, 
 * allow the prediction generation service to use the (then updated) TDS data, as well as cache the result of the time
 * prediction generation service between updates.
 * 
 * @author jmaki
 *
 */
class StopBasedPredictionIntegrationService implements PredictionIntegrationService {

  @Autowired
  private PredictionGenerationService _predictionGenerationService;

  public void setPredictionGenerationService(PredictionGenerationService predictionGenerationService) {
    _predictionGenerationService = predictionGenerationService;
  }
  
  private Map<String, List<TimepointPredictionRecord>> _predictionRecordsByVehicle =
      new HashMap<String, List<TimepointPredictionRecord>>();
  
  @Override
  public void updatePredictionsForVehicle(AgencyAndId vehicleId) {
    _predictionRecordsByVehicle.put(AgencyAndId.convertToString(vehicleId), 
        _predictionGenerationService.getPredictionsForVehicle(vehicleId));
  }

  @Override
  public List<TimepointPredictionRecord> getPredictionsForTrip(TripStatusBean tripStatus) {
    return _predictionRecordsByVehicle.get(tripStatus.getVehicleId());
  }
  
  public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String VehicleId, String TripId) {
    return _predictionRecordsByVehicle.get(VehicleId);
  }

}
