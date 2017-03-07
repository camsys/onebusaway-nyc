package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TripUpdateServiceImpl extends AbstractFeedMessageService {

    private TripUpdateFeedBuilder _feedBuilder;
    private NycTransitDataService _transitDataService;

    private static final Logger _log = LoggerFactory.getLogger(TripUpdateServiceImpl.class);

    @Autowired
    public void setFeedBuilder(TripUpdateFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Override
    protected List<FeedEntity> getEntities() {
        long time = getTime();
        ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(getAgencyId(), time);

        List<FeedEntity> entities = new ArrayList<FeedEntity>();

        for (VehicleStatusBean vehicle : vehicles.getList()) {

            if (vehicle.getTrip() == null)
                continue;

            List<TimepointPredictionRecord> tprs = _transitDataService.getPredictionRecordsForVehicleAndTrip(vehicle.getVehicleId(), vehicle.getTrip().getId());

            if (tprs == null)
                continue;

            GtfsRealtime.TripUpdate tu  = _feedBuilder.makeTripUpdate(vehicle, tprs);

            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setTripUpdate(tu);
            entity.setId(tu.getTrip().getTripId());
            entities.add(entity.build());

        }

        return entities;
    }

}
