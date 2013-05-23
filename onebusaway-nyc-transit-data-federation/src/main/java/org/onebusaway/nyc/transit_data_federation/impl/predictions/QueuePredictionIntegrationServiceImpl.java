package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.TimeQueueListenerTask;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionGenerationService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

/**
 * Listen for (queue), cache, and return time predictions. 
 * @author sheldonabrown
 *
 */
public class QueuePredictionIntegrationServiceImpl extends
		TimeQueueListenerTask implements PredictionIntegrationService {

  @Autowired
  private NycTransitDataService _transitDataService;
  
  
  private Cache<String, List<TimepointPredictionRecord>> cache = CacheBuilder.newBuilder()
      .expireAfterWrite(2, TimeUnit.MINUTES)
      .build();

	@Override
	protected void processResult(FeedMessage message) {
	  List<TimepointPredictionRecord> predictionRecords = new ArrayList<TimepointPredictionRecord>();  
	  String tripId = null;
	  String vehicleId = null;
	  Map<String, Long> stopTimeMap = new HashMap<String, Long>();
	  // convert FeedMessage to TimepointPredictionRecord
	  for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {
	    
	    TripUpdate tu = entity.getTripUpdate();
	    if (!tu.getVehicle().getId().equals(vehicleId)
	        || !tu.getTrip().getTripId().equals(tripId)) {
	      stopTimeMap = loadScheduledTimes(tu.getVehicle().getId(), tu.getTrip().getTripId());
	    }
	    tripId = tu.getTrip().getTripId();
	    vehicleId = tu.getVehicle().getId();

	    for (StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
	      TimepointPredictionRecord tpr = new TimepointPredictionRecord();
	      // this validates the Agency_StopID convention
	      tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(stu.getStopId())); 
	      tpr.setTimepointPredictedTime(stu.getArrival().getTime());
	      Long scheduledTime = stopTimeMap.get(stu.getStopId());
	      
	      if (scheduledTime != null) {
	        tpr.setTimepointScheduledTime(scheduledTime);
	        predictionRecords.add(tpr);
	      }
	    }
	    
	  }
    
	  if (vehicleId != null) {
	    // place in cache if we were able to extract a vehicle id
	    this.cache.put(vehicleId, predictionRecords);
	  }
	}
	
	private Map<String, Long> loadScheduledTimes(String vehicleId, String tripId) {
	  Map<String, Long> map = new HashMap<String, Long>();
    VehicleStatusBean vehicleStatus = _transitDataService.getVehicleForAgency(vehicleId, System.currentTimeMillis());
    
    if(vehicleStatus == null) {
      return map;
    }

    TripStatusBean tripStatus = vehicleStatus.getTripStatus();
    if(tripStatus == null) {
      return map;
    }
    
    BlockInstanceBean blockInstance = _transitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());
    if(blockInstance == null) {
      return map;
    }
    
    // we need to match the given trip to the active trip
    List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();
    boolean foundActiveTrip = false;
    for (BlockTripBean blockTrip : blockTrips) {
      if (!foundActiveTrip) {
        if(tripId.equals(blockTrip.getTrip().getId())) {
          for (BlockStopTimeBean bst : blockTrip.getBlockStopTimes()) {
            map.put(bst.getStopTime().getStop().getId(), tripStatus.getServiceDate() + (bst.getStopTime().getArrivalTime() * 1000));
          }
        }
      }
    }
    return map;
	}

  @Override
  public void updatePredictionsForVehicle(AgencyAndId vehicleId) {
    // no op, messages come in from queue
  }

  @Override
  public List<TimepointPredictionRecord> getPredictionsForTrip(
      TripStatusBean tripStatus) {
    return this.cache.getIfPresent(tripStatus.getVehicleId());
  }

}
