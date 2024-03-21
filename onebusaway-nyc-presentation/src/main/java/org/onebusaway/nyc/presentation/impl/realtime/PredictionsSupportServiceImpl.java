/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.nyc.presentation.service.realtime.PredictionsSupportService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PredictionsSupportServiceImpl implements PredictionsSupportService {

    @Autowired
    private ConfigurationService _configurationService;

    @Autowired
    private NycTransitDataService _nycTransitDataService;

    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    public void setNycTransitDataService(NycTransitDataService nycTransitDataService) {
        _nycTransitDataService = nycTransitDataService;
    }

    @Override
    public Map<String, SiriSupportPredictionTimepointRecord> getStopIdToPredictionRecordMap(TripStatusBean currentTripStatusBean){
        Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap = new HashMap<>();

        List<TimepointPredictionRecord> currentTripPredictions = _nycTransitDataService.getPredictionRecordsForVehicleAndTripStatus(currentTripStatusBean.getVehicleId(), currentTripStatusBean);
        List<TimepointPredictionRecord> nextTripPredictions = null;

        TripBean nextTripBean = null;

        if(Boolean.parseBoolean(_configurationService.getConfigurationValueAsString(
                "display.showNextTripPredictions", "false"))) {
            nextTripBean = getNextTrip(currentTripStatusBean);
            if (nextTripBean != null) {
                nextTripPredictions = _nycTransitDataService.getPredictionRecordsForVehicleAndTrip(currentTripStatusBean.getVehicleId(), nextTripBean.getId());
            }
        }

        // (build map of stop IDs to TPRs)
        if(currentTripPredictions != null) {
            for(TimepointPredictionRecord tpr : currentTripPredictions) {
                SiriSupportPredictionTimepointRecord r = new SiriSupportPredictionTimepointRecord();
                r.setStopId(tpr.getTimepointId().toString());
                r.setTripId(currentTripStatusBean.getActiveTrip().getId());
                r.setTimepointPredictionRecord(tpr);
                stopIdToPredictionRecordMap.put(r.getKey(), r);
            }
        }
        if(nextTripPredictions != null && nextTripBean != null){
            for(TimepointPredictionRecord tpr : nextTripPredictions) {
                SiriSupportPredictionTimepointRecord r = new SiriSupportPredictionTimepointRecord();
                r.setStopId(tpr.getTimepointId().toString());
                r.setTripId(nextTripBean.getId());
                r.setTimepointPredictionRecord(tpr);
                stopIdToPredictionRecordMap.put(r.getKey(), r);
            }
        }
        return stopIdToPredictionRecordMap;
    }

    @Override
    public TripBean getNextTrip(TripStatusBean currentVehicleTripStatus){
        BlockInstanceBean blockInstance =
                _nycTransitDataService.getBlockInstance(currentVehicleTripStatus.getActiveTrip().getBlockId(), currentVehicleTripStatus.getServiceDate());

        List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();
        TripBean nextTripBean = null;
        for(int i=0; i<blockTrips.size(); i++){
            //does this block trip match the active trip currently in progress, if so, lets get the next trip if it exists
            if(blockTrips.get(i).getTrip().getId().equals(currentVehicleTripStatus.getActiveTrip().getId())){
                if(i+1 < blockTrips.size()){
                    if(blockTrips.get(i+1) != null){
                        nextTripBean = blockTrips.get(i+1).getTrip();
                        break;
                    }
                }
            }
        }
        return nextTripBean;
    }

}
