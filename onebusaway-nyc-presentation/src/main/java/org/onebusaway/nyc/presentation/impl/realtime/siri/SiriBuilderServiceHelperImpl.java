/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.presentation.impl.realtime.siri;

import org.onebusaway.nyc.presentation.service.realtime.siri.SiriBuilderServiceHelper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiriBuilderServiceHelperImpl implements SiriBuilderServiceHelper {

    private NycTransitDataService _nycTransitDataService;

    @Autowired
    public void setNycTransitDataService(NycTransitDataService nycTransitDataService) {
        _nycTransitDataService = nycTransitDataService;
    }

    @Override
    public boolean isNonRevenueStop(TripStatusBean currentlyActiveTripOnBlock, BlockStopTimeBean stopTime){
        if(currentlyActiveTripOnBlock.getActiveTrip().getRoute() != null) {
            String agencyId = currentlyActiveTripOnBlock.getActiveTrip().getRoute().getAgency().getId();
            String routeId = currentlyActiveTripOnBlock.getActiveTrip().getRoute().getId();
            String directionId = currentlyActiveTripOnBlock.getActiveTrip().getDirectionId();
            String stopId  = stopTime.getStopTime().getStop().getId();
            if (!_nycTransitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId, routeId, directionId)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Double getDistanceOfVehicleAlongBlock(TripStatusBean currentlyActiveTripOnBlock, BlockTripBean blockTrip) {
        if(currentlyActiveTripOnBlock.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
            return blockTrip.getDistanceAlongBlock()  + currentlyActiveTripOnBlock.getDistanceAlongTrip();
        }
        return null;
    }
}
