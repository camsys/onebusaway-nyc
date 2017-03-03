package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VehicleUpdateServiceImpl extends AbstractFeedMessageService {

    private VehicleUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;
    private String _agencyId = "";

    @Autowired
    public void setFeedBuilder(VehicleUpdateFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    public void setAgencyId(String agencyId) {
        _agencyId = agencyId;
    }

    @Override
    protected List<FeedEntity> getEntities() {
        ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(_agencyId, getTime());
        long time = getTime();

        List<FeedEntity> entities = new ArrayList<FeedEntity>();

        for (VehicleStatusBean vehicle : vehicles.getList()) {
            VehicleLocationRecordBean vlr = _transitDataService.getVehicleLocationRecordForVehicleId(vehicle.getVehicleId(), time);
            if (vlr != null) {
                VehiclePosition pos = _feedBuilder.getVehicleUpdateFromInferredLocation(vlr);
                FeedEntity.Builder entity = FeedEntity.newBuilder();
                entity.setVehicle(pos);
                entity.setId(pos.getVehicle().getId());
                entities.add(entity.build());
            }
        }

        return entities;
    }
}
