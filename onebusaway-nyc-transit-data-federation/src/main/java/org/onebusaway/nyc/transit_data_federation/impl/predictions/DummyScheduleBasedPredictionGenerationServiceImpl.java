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

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionGenerationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A component used for testing only that generates "predictions" based on the scheduled time to arrive at a stop.
 * In "real life", this would be replaced by a service that calls an actual prediction algorithm.
 * 
 * @author jmaki
 *
 */

class DummyScheduleBasedPredictionGenerationServiceImpl implements PredictionGenerationService {

  protected static Logger _log = LoggerFactory.getLogger(DummyScheduleBasedPredictionGenerationServiceImpl.class);
  
  @Autowired
  private NycTransitDataService _transitDataService;

  @Override
  public List<TimepointPredictionRecord> getPredictionsForVehicle(AgencyAndId vehicleId) {
    _log.error("getPredictionsForVehicle");
    List<TimepointPredictionRecord> predictionRecords = new ArrayList<TimepointPredictionRecord>();

    VehicleStatusBean vehicleStatus = _transitDataService.getVehicleForAgency(AgencyAndId.convertToString(vehicleId), System.currentTimeMillis());
    if(vehicleStatus == null)
      return null;

    TripStatusBean tripStatus = vehicleStatus.getTripStatus();    
    if(tripStatus == null)
      return null;
    
    BlockInstanceBean blockInstance = _transitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());
    if(blockInstance == null)
      return null;
    
    List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

    double distanceOfVehicleAlongBlock = 0;
    boolean foundActiveTrip = false;
    for(int i = 0; i < blockTrips.size(); i++) {
      BlockTripBean blockTrip = blockTrips.get(i);

      if(!foundActiveTrip) {
        if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
          distanceOfVehicleAlongBlock += tripStatus.getDistanceAlongTrip();

          foundActiveTrip = true;
        } else {
          // a block trip's distance along block is the *beginning* of that block trip along the block
          // so to get the size of this one, we have to look at the next.
          if(i + 1 < blockTrips.size()) {
            distanceOfVehicleAlongBlock = blockTrips.get(i + 1).getDistanceAlongBlock();
          }

          // bus has already served this trip, so no need to go further
          continue;
        }
      }

      for(BlockStopTimeBean blockStopTime : blockTrip.getBlockStopTimes()) {
        if(blockStopTime.getDistanceAlongBlock() < distanceOfVehicleAlongBlock) {
          continue;
        }

        TimepointPredictionRecord tpr = new TimepointPredictionRecord();
        tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(blockStopTime.getStopTime().getStop().getId()));
        tpr.setTimepointScheduledTime(tripStatus.getServiceDate() + blockStopTime.getStopTime().getArrivalTime() * 1000);
        tpr.setTimepointPredictedTime(Math.round(tripStatus.getServiceDate() + (blockStopTime.getStopTime().getArrivalTime() * 1000) + (tripStatus.getScheduleDeviation() * 1000)));
        
        predictionRecords.add(tpr);
      }

      break;
    }
    
    return predictionRecords;
  }
  
}
