package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.TimeQueueListenerTask;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionCacheService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.time.SystemTime;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

/**
 * Listen for (queue), cache, and return time predictions.
 * 
 * @author sheldonabrown
 *
 */
public class QueuePredictionIntegrationServiceImpl extends
		TimeQueueListenerTask implements PredictionIntegrationService {

	private static final String DEFAULT_FALSE = "false";

	private static final String PREDICTION_AGE_LIMT = "display.predictionAgeLimit";
	private static final String CHECK_PREDICTION_AGE = "display.checkPredictionAge";
	private static final String CHECK_PREDICTION_LATENCY = "display.checkPredictionLatency";
	private static final String PREDICTION_QUEUE_THREAD_COUNT = "tds.predictionQueueThreadCount";
	private static final int DEFAULT_PREDICTION_QUEUE_THREAD_COUNT = 4;
	
	private static final long MESSAGE_WAIT_MILLISECONDS = 1 * 1000;

	private Boolean _checkPredictionAge;
	private Boolean _checkPredictionLatency;
	private Integer _predictionAgeLimit = 300;
	private int _predictionResultThreads = DEFAULT_PREDICTION_QUEUE_THREAD_COUNT;

	Date markTimestamp = new Date();
	int processedCount = 0;
	int _countInterval = 10000;
	

	private ArrayBlockingQueue<FeedMessage> _predictionMessageQueue = new ArrayBlockingQueue<FeedMessage>(
		      1000000);
	
	private ExecutorService _predictionExecutorService;
	
	private PredictionResultTask[] _predictionResultTask;
	private Future<?>[] _predictionResultFuture;
	


	@Autowired
	private NycTransitDataService _transitDataService;

	@Autowired
	private ConfigurationService _configurationService;
	
	@Autowired
	private PredictionCacheService _cacheService;

	private Cache<String, List<TimepointPredictionRecord>> getCache() {
		return _cacheService.getCache();
	}

	@Refreshable(dependsOn = { CHECK_PREDICTION_LATENCY,CHECK_PREDICTION_AGE, PREDICTION_AGE_LIMT })
	private synchronized void refreshConfig() {
		_checkPredictionAge = Boolean.parseBoolean(_configurationService
				.getConfigurationValueAsString(CHECK_PREDICTION_AGE,
						DEFAULT_FALSE));
		_checkPredictionLatency = Boolean.parseBoolean(_configurationService
				.getConfigurationValueAsString(CHECK_PREDICTION_LATENCY,
						DEFAULT_FALSE));
		_predictionAgeLimit = Integer.parseInt(_configurationService
				.getConfigurationValueAsString(PREDICTION_AGE_LIMT, "300"));
	}
	
	@Refreshable(dependsOn = { PREDICTION_QUEUE_THREAD_COUNT })
	public synchronized void refreshPredictionQueueThreadCount() {
		_predictionResultThreads = _configurationService.
				getConfigurationValueAsInteger(PREDICTION_QUEUE_THREAD_COUNT, DEFAULT_PREDICTION_QUEUE_THREAD_COUNT);
		reinitializePredictionQueue();
	}
	
	@PostConstruct
	@Override
	public void setup() {
		super.setup();
		refreshPredictionQueueThreadCount();
		initializePredictionQueue();	
	}
	
	@PreDestroy
	@Override
	public void destroy() {
		try{
			_predictionMessageQueue.clear();
			shutdownPredictionQueue();
			super.destroy();
		}
		catch(InterruptedException ie){
			_log.error("Failed to cleanly shutdown the predictionExecutorService: ", ie);
		}
	}
	
	public synchronized void initializePredictionQueue(){
		_predictionExecutorService = Executors.newFixedThreadPool(_predictionResultThreads);
		_predictionResultTask = new PredictionResultTask[_predictionResultThreads];
		_predictionResultFuture = new Future<?>[_predictionResultThreads];
		for (int i = 0; i < _predictionResultThreads; i++) {
			PredictionResultTask task = new PredictionResultTask(i);
			_predictionResultFuture[i] = _predictionExecutorService.submit(task);
			_predictionResultTask[i] = task;
		}
	}
	
	public synchronized void shutdownPredictionQueue() throws InterruptedException{
		try{
			for (int i = 0; i < _predictionResultThreads; i++) {
				_predictionResultTask[i].notifyShutdown();
				_predictionResultFuture[i].cancel(true);
			}
		}
		catch (Exception e){
			_log.error("Read thread did not complete cleanly: " + e.getMessage());
		}
		finally{
			if(_predictionExecutorService != null){
				_predictionExecutorService.shutdownNow();
				_predictionExecutorService.awaitTermination(10, TimeUnit.SECONDS);
			}
		}
	}
	
	public void reinitializePredictionQueue() {
		try{
			shutdownPredictionQueue();
			initializePredictionQueue();	
		}
		catch (InterruptedException ie) {
			_log.error("Unable to reinitialize queue: ", ie);
			return;
		}
	}

	@Override
	protected void processResult(FeedMessage message) {
		try{
			boolean success = _predictionMessageQueue.offer(message, MESSAGE_WAIT_MILLISECONDS,
			        TimeUnit.MILLISECONDS);
		    if (!success)
		      _log.warn("QUEUE FULL, dropped message");
		}
		catch(Exception e){
			_log.error("Error occured when attempting to queue message: ", e);
		}
	}
	
    private class PredictionResultTask implements Runnable {
    	private int threadNumber;
    	private int predictionRecordCount = 0;
    	private int predictionRecordCountInterval = 2000;
    	private long predictionRecordAverageLatency = 0;
    	private boolean _notifyShutdown = false;
    	
    	public PredictionResultTask(int threadNumber) {
    		this.threadNumber = threadNumber;
    	}
    	
    	public void run() {
	    	while (!_notifyShutdown) {
	    		try {
	    			 
	    			 FeedMessage message =_predictionMessageQueue.take();
	    			 
	    			 if(message != null){
		    			 
	    				 if (enableCheckPredictionLatency()) {
		    				 logPredictionLatency(message);
		    			 }
		    			 
		    			 if (enableCheckPredictionAge() && 
		    					 isPredictionRecordPastAgeLimit(message)) {
		    				 // Drop old messages	 
		    				 continue;
		    			 }

		    			 List<TimepointPredictionRecord> predictionRecords = new ArrayList<TimepointPredictionRecord>();  
	    		  	  	 Map<String, Long> stopTimeMap = new HashMap<String, Long>();
	    		  	  	 
	    		  	  	 String tripId = null;
	    		  	  	 String vehicleId = null;
	    		  	  	 
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
	    		 }
	    		 catch(Exception e){
	    			 _log.error("exception sending: ", e);
	    		 }
	    	}
	    	_log.error("PredictionResultTask thread loop interrupted, exiting");
    	}
    	
    	public void notifyShutdown() {
    		_notifyShutdown = true;
    	}
    	
    	private boolean enableCheckPredictionLatency() {
    		if (_checkPredictionLatency == null) {
    			refreshConfig();
    		}
    		return _checkPredictionLatency;
    	}
    	
    	private void logPredictionLatency(FeedMessage message){
    		long currentTime = SystemTime.currentTimeMillis();
    		Long messageTimeStamp = message.getHeader().getTimestamp();
    		if (messageTimeStamp != null && messageTimeStamp > 0) {
				predictionRecordCount++;
				predictionRecordAverageLatency += (currentTime - messageTimeStamp);
				if (predictionRecordCount >= predictionRecordCountInterval) {
					String avgPredictionRecordLatencyAsText = getHumanReadableElapsedTime(predictionRecordAverageLatency
							/ predictionRecordCountInterval);
					_log.info("Average predictions message latency for predictions thread " + getThreadNumber() +  " is: "
							+ avgPredictionRecordLatencyAsText);
					predictionRecordCount = 0;
					predictionRecordAverageLatency = (long) 0;
				}
			}
    	}
    	
    	private String getHumanReadableElapsedTime(long timestamp) {
    		return String.format(
    				"%02d min, %02d sec",
    				TimeUnit.MILLISECONDS.toMinutes(timestamp),
    				TimeUnit.MILLISECONDS.toSeconds(timestamp)
    						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
    								.toMinutes(timestamp)));
    	}
    	
    	private boolean enableCheckPredictionAge() {
    		if (_checkPredictionAge == null) {
    			refreshConfig();
    		}
    		return _checkPredictionAge;
    	}
    	
    	private boolean isPredictionRecordPastAgeLimit(FeedMessage message){
    		long difference = computeTimeDifference(message.getHeader().getTimestamp());
			if (difference > _predictionAgeLimit) {
				if (message.getEntity(0).getVehicle() != null)
					_log.info("Prediction Trip Update for "
							+ message.getEntity(0).getTripUpdate().getVehicle()
									.getId() + " discarded.");
				return true;
			}
			return false;
    	}
    	
    	protected long computeTimeDifference(long timestamp) {
    		return (SystemTime.currentTimeMillis() - timestamp) / 1000; // output in seconds															
    	}
    	
    	private Map<String, Long> loadScheduledTimes(String vehicleId, String tripId) {
    		
    		Map<String, Long> map = new HashMap<String, Long>();
    		VehicleStatusBean vehicleStatus = _transitDataService
    				.getVehicleForAgency(vehicleId, SystemTime.currentTimeMillis());

    		if (vehicleStatus == null) {
    			return map;
    		}

    		TripStatusBean tripStatus = vehicleStatus.getTripStatus();
    		if (tripStatus == null) {
    			return map;
    		}
    			
    		BlockInstanceBean blockInstance = _transitDataService.getBlockInstance(
    				tripStatus.getActiveTrip().getBlockId(),
    				tripStatus.getServiceDate());
    	
    		if (blockInstance == null) {
    			return map;
    		}

    		// we need to match the given trip to the active trip
    		List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration()
    				.getTrips();
    		boolean foundActiveTrip = false;
    		for (BlockTripBean blockTrip : blockTrips) {
    			if (!foundActiveTrip) {
    				if (tripId.equals(blockTrip.getTrip().getId())) {
    					for (BlockStopTimeBean bst : blockTrip.getBlockStopTimes()) {
    						map.put(bst.getStopTime().getStop().getId(),
    								tripStatus.getServiceDate()
    										+ (bst.getStopTime().getArrivalTime() * 1000));
    					}
    				}
    			}
    		}
    		
    		return map;
    	}
    	
    	public int getThreadNumber(){
    		return this.threadNumber;
    	}
    }
	
	@Override
	public void updatePredictionsForVehicle(AgencyAndId vehicleId) {
		// no op, messages come in from queue
	}

	private String hash(String vehicleId, String tripId) {
		return vehicleId + "-" + tripId;
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