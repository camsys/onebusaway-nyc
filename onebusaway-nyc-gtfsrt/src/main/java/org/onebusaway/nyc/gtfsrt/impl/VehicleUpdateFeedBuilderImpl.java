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

import com.google.transit.realtime.GtfsRealtimeCrowding;
import com.google.transit.realtime.GtfsRealtimeOneBusAway;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;

import org.onebusaway.transit_data.model.trips.VehicleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.*;

@Component
public class VehicleUpdateFeedBuilderImpl implements VehicleUpdateFeedBuilder {

    private static final Logger _log = LoggerFactory.getLogger(VehicleUpdateFeedBuilderImpl.class);

	private NycTransitDataService _transitDataService;

	private ConfigurationService _configurationService;

	@Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService){
	    _configurationService = configurationService;
    }
    @Override
    public VehiclePosition.Builder makeVehicleUpdate(VehicleStatusBean status, VehicleLocationRecordBean record, 
        VehicleOccupancyRecord vor) {

        VehiclePosition.Builder position = VehiclePosition.newBuilder();
        position.setTimestamp(record.getTimeOfRecord()/1000);
        position.setPosition(makePosition(record));
        position.setVehicle(makeVehicleDescriptor(record));

        Set<VehicleFeature> features = status.getTripStatus().getVehicleFeatures();
        for(VehicleFeature feature : features){
            if(feature.equals(VehicleFeature.STROLLER)){
                GtfsRealtimeOneBusAway.OneBusAwayVehicleDescriptor.Builder obaVehicleDescriptor =
                        GtfsRealtimeOneBusAway.OneBusAwayVehicleDescriptor.newBuilder();
                obaVehicleDescriptor.addVehicleFeature("STROLLER");
                position.getVehicleBuilder().setExtension(GtfsRealtimeOneBusAway.obaVehicleDescriptor, obaVehicleDescriptor.build());
            }
        }
        if(showApc() && getGtfsrtOccupancy(status) != null){
            position.setOccupancyStatus(getGtfsrtOccupancy(status));
        }

        // if we have count or capacity, create custom extension
        if (vor != null
                && showApc()
                && (vor.getCapacity() != null || vor.getRawCount() != null)) {
            GtfsRealtimeCrowding.CrowdingDescriptor.Builder builder = GtfsRealtimeCrowding.CrowdingDescriptor.newBuilder();
            if (vor.getRawCount() != null)
                builder.setEstimatedCount(vor.getRawCount());
            if (vor.getCapacity() != null)
                builder.setEstimatedCapacity(vor.getCapacity());
            position.setExtension(GtfsRealtimeCrowding.crowdingDescriptor, builder.build());
            _log.info(" v:" + record.getVehicleId() + " vor=" + vor.getRawCount() + "/" + vor.getCapacity());
        }


        if (EVehiclePhase.SPOOKING.equals(EVehiclePhase.valueOf(record.getPhase().toUpperCase()))) {
            return position;
        }

        position.setTrip(makeTripDescriptor(status));


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

    public boolean showApc(){
        if(!useApc()){
            return false;
        }
        String apc = _configurationService.getConfigurationValueAsString("display.validApcKeys", "");
        List<String> keys = Arrays.asList(apc.split("\\s*;\\s*"));
        for(String key : keys){
            if(key.trim().equals("*")){
                return true;
            }
        }
        return false;
    }

    private boolean useApc(){
        return _configurationService.getConfigurationValueAsBoolean("tds.useApc", Boolean.FALSE);
    }
}
