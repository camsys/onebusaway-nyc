package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.TimeQueueListenerTask;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

  private static final int DEFAULT_CACHE_TIMEOUT = 2 * 60; // seconds
  private static final String CACHE_TIMEOUT_KEY = "tds.prediction.expiry";
  
  @Autowired
  private NycTransitDataService _transitDataService;
  
  @Autowired
  private ConfigurationService _configurationService;
  
  
  private Cache<String, List<TimepointPredictionRecord>> _cache = null;
  
  private synchronized Cache<String, List<TimepointPredictionRecord>> getCache() {
    if (_cache == null) {
      
      int timeout = _configurationService.getConfigurationValueAsInteger(CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
      _log.info("creating initial prediction cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    return _cache;
  }

  @Refreshable(dependsOn = {CACHE_TIMEOUT_KEY})
  private synchronized void refreshCache() {
    if (_cache == null) return; // nothing to do
    int timeout = _configurationService.getConfigurationValueAsInteger(CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    _log.info("rebuilding prediction cache with " + _cache.size() + " entries after refresh with timeout=" + timeout + "...");
    ConcurrentMap<String, List<TimepointPredictionRecord>> map = _cache.asMap();
    _cache = CacheBuilder.newBuilder()
        .expireAfterWrite(timeout, TimeUnit.SECONDS)
        .build();
    for (Entry<String, List<TimepointPredictionRecord>> entry : map.entrySet()) {
      _cache.put(entry.getKey(), entry.getValue());
    }
    _log.info("done");
  }
  
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
	    
	    if (vehicleId != null) {
		    // place in cache if we were able to extract a vehicle id
		    getCache().put(hash(vehicleId, tripId), predictionRecords);
		}
	    
	  }
	}
	
	private String hash(String vehicleId, String tripId) {
	  return vehicleId + "-" + tripId;
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
    return getCache().getIfPresent(hash(tripStatus.getVehicleId(), tripStatus.getActiveTrip().getId()));
  }
  
  public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(
      String VehicleId, String TripId) {
    return getCache().getIfPresent(hash(VehicleId, TripId));
  }

}
