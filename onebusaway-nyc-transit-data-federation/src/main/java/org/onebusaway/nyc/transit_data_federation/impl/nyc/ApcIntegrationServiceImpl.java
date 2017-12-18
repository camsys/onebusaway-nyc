/**
 * Copyright (C) 2017 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.ApcQueueListenerTask;
import org.onebusaway.realtime.api.VehicleOccupancyListener;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class ApcIntegrationServiceImpl extends ApcQueueListenerTask {

    @Autowired
    private NycTransitDataService _tds;

    @Autowired
    private VehicleOccupancyListener _listener;

    @Override
    protected void processResult(NycVehicleLoadBean message, String contents) {
        _log.debug("found message=" + message + " for contents=" + contents);
        VehicleOccupancyRecord vor = toVehicleOccupancyRecord(message);
        _listener.handleVehicleOccupancyRecord(vor);
    }

    private VehicleOccupancyRecord toVehicleOccupancyRecord(NycVehicleLoadBean message) {
        VehicleOccupancyRecord vor = new VehicleOccupancyRecord();
        vor.setOccupancyStatus(message.getLoad());
        vor.setTimestamp(new Date(message.getRecordTimestamp()));
        vor.setVehicleId(AgencyAndId.convertFromString(message.getVehicleId()));
        vor.setRouteId(message.getRoute());
        vor.setDirectionId(message.getDirection());
        return vor;
    }
}
