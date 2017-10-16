/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {
	
	private NycTransitDataService _transitDataService;
	
	@Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Override
    public VehiclePosition.Builder makeVehicleUpdate(VehicleStatusBean status, VehicleLocationRecordBean record, 
        OccupancyStatus occupancy) {
        VehiclePosition.Builder position = VehiclePosition.newBuilder();
        position.setTrip(makeTripDescriptor(status));
        position.setPosition(makePosition(record));
        position.setVehicle(makeVehicleDescriptor(record));
        position.setTimestamp(record.getTimeOfRecord()/1000);
        
        if(getGtfsrtOccupancy(status) != null){
            position.setOccupancyStatus(getGtfsrtOccupancy(status));
        }
        
        if (status.getTripStatus().getNextStop() != null) {
            AgencyAndId id = AgencyAndId.convertFromString(status.getTripStatus().getNextStop().getId());
            position.setStopId(id.getId());
        }

        return position;
    }
    
    private com.google.transit.realtime.GtfsRealtime.VehiclePosition.OccupancyStatus getGtfsrtOccupancy(VehicleStatusBean status){
       if(status.getOccupancyStatus() == null)
          return null;
       return com.google.transit.realtime.GtfsRealtime.VehiclePosition.OccupancyStatus.valueOf(status.getOccupancyStatus().valueOf());
    }
}
