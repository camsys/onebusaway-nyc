package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VehicleUpdateServiceImpl extends AbstractFeedMessageService {

    private VehicleUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;

    private static final Logger _log = LoggerFactory.getLogger(VehicleUpdateServiceImpl.class);

    @Autowired
    public void setFeedBuilder(VehicleUpdateFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Override
    protected List<FeedEntity.Builder> getEntities(long time) {
        ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(getAgencyId(), time);

        List<FeedEntity.Builder> entities = new ArrayList<FeedEntity.Builder>();

        long nMissing = 0;

        for (VehicleStatusBean vehicle : vehicles.getList()) {

            if (vehicle.getTrip() == null) {
                continue;
            }

            VehicleLocationRecordBean vlr = _transitDataService.getVehicleLocationRecordForVehicleId(vehicle.getVehicleId(), time);
            if (vlr == null) {
                nMissing++;
                continue;
            }

            VehiclePosition.Builder pos = _feedBuilder.makeVehicleUpdate(vehicle, vlr);

            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setVehicle(pos);
            entity.setId(pos.getVehicle().getId());
            entities.add(entity);

        }

        _log.info("{} VehicleStatusBeans are missing VLRs", nMissing);

        return entities;
    }

}
