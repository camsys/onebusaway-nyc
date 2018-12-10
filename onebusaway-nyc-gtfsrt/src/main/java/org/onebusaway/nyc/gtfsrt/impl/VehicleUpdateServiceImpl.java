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

import com.google.transit.realtime.GtfsRealtime.*;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Build VehicleUpdate feed for current time.
 */
@Component
public class VehicleUpdateServiceImpl extends AbstractFeedMessageService {

    private VehicleUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;
    private PresentationService _presentationService;

    private static final Logger _log = LoggerFactory.getLogger(VehicleUpdateServiceImpl.class);

    @Autowired
    public void setFeedBuilder(VehicleUpdateFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Autowired
    public void setPresentationService(PresentationService presentationService) {
        _presentationService = presentationService;
    }

    @Override
    public List<FeedEntity.Builder> getEntities(long time) {
        Collection<VehicleStatusBean> vehicles = getAllVehicles(_transitDataService, _presentationService, time);

        List<FeedEntity.Builder> entities = new ArrayList<FeedEntity.Builder>();

        long nMissing = 0;

        for (VehicleStatusBean vehicle : vehicles) {

            if (!includeVehicle(_transitDataService, _presentationService, vehicle, time)) {
                continue;
            }

            VehicleLocationRecordBean vlr = _transitDataService.getVehicleLocationRecordForVehicleId(vehicle.getVehicleId(), time);
            if (vlr == null) {
                nMissing++;
                continue;
            }

            OccupancyStatus occupancy = getOccupancyStatus(vehicle);
            VehiclePosition.Builder pos = _feedBuilder.makeVehicleUpdate(vehicle, vlr, occupancy);

            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setVehicle(pos);
            entity.setId(pos.getVehicle().getId());
            entities.add(entity);

        }

        _log.info("{} VehicleStatusBeans are missing VLRs", nMissing);

        return entities;
    }

    private OccupancyStatus getOccupancyStatus(VehicleStatusBean vehicleStatus){
      TripStatusBean tripStatus = vehicleStatus.getTripStatus();
      
      if(tripStatus != null){
        VehicleOccupancyRecord vor =
            _transitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(
                AgencyAndId.convertFromString(tripStatus.getVehicleId()), 
                tripStatus.getActiveTrip().getRoute().getId(),
                tripStatus.getActiveTrip().getDirectionId());
      
        if(vor != null)
          return vor.getOccupancyStatus();
      }
      
      return null;
      
    }

}
