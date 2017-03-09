package org.onebusaway.nyc.gtfsrt.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private Cache<String, BlockInstanceBean> _blockCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    private BlockInstanceBean getBlock(VehicleStatusBean vehicle) {
        String blockId = vehicle.getTrip().getBlockId();
        long serviceDate = vehicle.getTripStatus().getServiceDate();
        String key = blockId + " " + Long.toString(serviceDate);
        BlockInstanceBean block = _blockCache.getIfPresent(key);
        if (block == null) {
            block =  _transitDataService.getBlockInstance(blockId, serviceDate);
            _blockCache.put(key, block);
        }
        return block;
    }

    @Override
    protected List<FeedEntity.Builder> getEntities() {
        long time = getTime();
        ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(getAgencyId(), time);

        List<FeedEntity.Builder> entities = new ArrayList<FeedEntity.Builder>();

        for (VehicleStatusBean vehicle : vehicles.getList()) {

            if (vehicle.getTrip() == null)
                continue;

            int tripSequence = vehicle.getTripStatus().getBlockTripSequence();
            BlockInstanceBean block = getBlock(vehicle);
            List<BlockTripBean> trips = block.getBlockConfiguration().getTrips();

            for (int i = tripSequence; i < trips.size(); i++) {
                TripBean trip = trips.get(i).getTrip();
                List<TimepointPredictionRecord> tprs = _transitDataService.getPredictionRecordsForVehicleAndTrip(vehicle.getVehicleId(), trip.getId());
                if (tprs == null)
                    break;

                GtfsRealtime.TripUpdate.Builder tu  = _feedBuilder.makeTripUpdate(trip, vehicle, tprs);

                FeedEntity.Builder entity = FeedEntity.newBuilder();
                entity.setTripUpdate(tu);
                entity.setId(tu.getTrip().getTripId());
                entities.add(entity);
            }

        }

        return entities;
    }

}
