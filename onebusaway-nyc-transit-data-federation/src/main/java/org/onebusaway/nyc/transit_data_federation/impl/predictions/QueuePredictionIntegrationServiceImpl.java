package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
 * 
 * @author sheldonabrown
 *
 */
public class QueuePredictionIntegrationServiceImpl extends
		TimeQueueListenerTask implements PredictionIntegrationService {

	private static final int DEFAULT_CACHE_TIMEOUT = 2 * 60; // seconds
	private static final String CACHE_TIMEOUT_KEY = "tds.prediction.expiry";

	private static final String DEFAULT_STOP_ID = "0";
	private static final String DEFAULT_VEHICLE_ID = "6379";
	private static final String DEFAULT_QUEUE_COUNT = "false";

	private static final String LOG_VEHICLE_ID = "tds.prediction.logVehicleId";
	private static final String LOG_STOP_ID = "tds.prediction.logStopId";
	private static final String LOG_QUEUE_COUNT = "tds.prediction.enableLogQueueCount";

	private static String _vehicleId;
	private static String _stopId;
	private Boolean _enableQueueCount;

	Date markTimestamp = new Date();
	int processedCount = 0;
	int _countInterval = 10000;

	@Autowired
	private NycTransitDataService _transitDataService;

	@Autowired
	private ConfigurationService _configurationService;

	private Cache<String, List<TimepointPredictionRecord>> _cache = null;

	private synchronized Cache<String, List<TimepointPredictionRecord>> getCache() {
		if (_cache == null) {

			int timeout = _configurationService.getConfigurationValueAsInteger(
					CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
			_log.info("creating initial prediction cache with timeout "
					+ timeout + "...");
			_cache = CacheBuilder.newBuilder()
					.expireAfterWrite(timeout, TimeUnit.SECONDS).build();
			_log.info("done");
		}
		return _cache;
	}

	@Refreshable(dependsOn = { CACHE_TIMEOUT_KEY })
	private synchronized void refreshCache() {
		if (_cache == null)
			return; // nothing to do
		int timeout = _configurationService.getConfigurationValueAsInteger(
				CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
		_log.info("rebuilding prediction cache with " + _cache.size()
				+ " entries after refresh with timeout=" + timeout + "...");
		ConcurrentMap<String, List<TimepointPredictionRecord>> map = _cache
				.asMap();
		_cache = CacheBuilder.newBuilder()
				.expireAfterWrite(timeout, TimeUnit.SECONDS).build();
		for (Entry<String, List<TimepointPredictionRecord>> entry : map
				.entrySet()) {
			_cache.put(entry.getKey(), entry.getValue());
		}
		_log.info("done");
	}

	@Refreshable(dependsOn = { LOG_VEHICLE_ID, LOG_STOP_ID, LOG_QUEUE_COUNT })
	private synchronized void refreshLoggingConfig() {
		_vehicleId = _configurationService.getConfigurationValueAsString(
				LOG_VEHICLE_ID, DEFAULT_VEHICLE_ID);
		_stopId = _configurationService.getConfigurationValueAsString(
				LOG_STOP_ID, DEFAULT_STOP_ID);
		_enableQueueCount = Boolean.parseBoolean(_configurationService
				.getConfigurationValueAsString(LOG_QUEUE_COUNT,
						DEFAULT_QUEUE_COUNT));
	}

	@Override
	protected void processResult(FeedMessage message) {

		List<TimepointPredictionRecord> predictionRecords = new ArrayList<TimepointPredictionRecord>();
		List<TimepointPredictionRecord> predictionRecordsNoSchedule = new ArrayList<TimepointPredictionRecord>();
		String tripId = null;
		String vehicleId = null;
		String routeId = null;
		Map<String, Long> stopTimeMap = new HashMap<String, Long>();
		// convert FeedMessage to TimepointPredictionRecord
		for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {

			TripUpdate tu = entity.getTripUpdate();

			// Why do we need to do this?
			if (!tu.getVehicle().getId().equals(vehicleId)
					|| !tu.getTrip().getTripId().equals(tripId)) {
				stopTimeMap = loadScheduledTimes(tu.getVehicle().getId(), tu
						.getTrip().getTripId());
			}

			tripId = tu.getTrip().getTripId();
			vehicleId = tu.getVehicle().getId();
			routeId = tu.getTrip().getRouteId();
			boolean containsStop = false;

			for (StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
				TimepointPredictionRecord tpr = new TimepointPredictionRecord();
				// this validates the Agency_StopID convention
				tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(stu
						.getStopId()));
				tpr.setTimepointPredictedTime(stu.getArrival().getTime());
				Long scheduledTime = stopTimeMap.get(stu.getStopId());

				String stopLogMessage = "";
				String vehicleLogMessage = "";

				if (containsId(stu.getStopId(), getStopId())) {
					containsStop = true;
					stopLogMessage += " Stop Id: " + stu.getStopId();
					stopLogMessage += " Route Id: " + routeId;
					stopLogMessage += " Time: "
							+ getTime(stu.getArrival().getTime());
					if (vehicleId != null) {
						stopLogMessage += " Vehicle Id: " + vehicleId;
					} else {
						stopLogMessage += " Vehicle Id is blank";
					}

					_log.info(stopLogMessage);
				}

				if (vehicleId != null) {

					if (containsId(vehicleId, getVehicleId())) {
						vehicleLogMessage += " Vehicle Id: " + vehicleId;
						vehicleLogMessage += " Route Id: " + routeId;
						vehicleLogMessage += " Stop Id: " + stu.getStopId();
						vehicleLogMessage += " Time: "
								+ getTime(stu.getArrival().getTime());

						_log.info(vehicleLogMessage);
					}
				} else {
					_log.warn("Vehicle ID for Stop Id: " + stu.getStopId()
							+ " returned null");
				}

				if (scheduledTime != null) {
					tpr.setTimepointScheduledTime(scheduledTime);
					predictionRecords.add(tpr);
				} else {
					predictionRecordsNoSchedule.add(tpr);
				}
			}

			if (vehicleId != null) {
				// place in cache if we were able to extract a vehicle id
				getCache().put(hash(vehicleId, tripId), predictionRecords);
			}

			// Logs all the vehicles in a stop that don't have predictions for
			// that stop
			if (predictionRecords.size() == 0 && containsStop) {
				_log.info("==================================================================================================");
				_log.warn("Vehicle ID " + vehicleId
						+ " has no prediction records");
				_log.warn("Trip ID " + tripId);
				_log.warn("Route ID " + routeId);
				_log.warn("Stop ID " + getStopId());
				TripUpdate tuInspect = tu;
				Map<String, Long> stopTimeMapInspect = stopTimeMap;
				for (StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
					Long scheduledTimeInspect = stopTimeMap
							.get(stu.getStopId());
				}
				_log.info("=================================================================================================");

			}

			if (predictionRecordsNoSchedule.size() > 1 && containsStop) {
				_log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				_log.warn("Found prediction records with no scheduled time");
				_log.warn("Vehicle ID " + vehicleId
						+ " has prediction records with no matching schedule");
				_log.warn("Trip ID " + tripId);
				_log.warn("Route ID " + routeId);
				_log.warn("Stop ID " + getStopId());

				TripUpdate tuInspect = tu;
				Map<String, Long> stopTimeMapInspect = stopTimeMap;
				
				VehicleStatusBean vehicleStatus = _transitDataService
						.getVehicleForAgency(vehicleId,
								System.currentTimeMillis());
				if (vehicleStatus == null){
					_log.warn("vehicleStatus is NULL");
					VehicleStatusBean vehicleStatusTemp = _transitDataService
							.getVehicleForAgency(vehicleId,
									System.currentTimeMillis());
					
					if(vehicleStatusTemp == null){
						_log.info("vehicleStatus is NULL");
					}
					else{
						_log.info(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
						_log.info("vehicleStatusTemp is NOT NULL");
						_log.info(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
					}
				}

				TripStatusBean tripStatus = vehicleStatus.getTripStatus();
				if (tripStatus == null)
					_log.warn("tripStatus is NULL");

				BlockInstanceBean blockInstance = _transitDataService
						.getBlockInstance(tripStatus.getActiveTrip()
								.getBlockId(), tripStatus.getServiceDate());
				if (blockInstance == null)
					_log.warn("blockInstance is NULL");

				List<BlockTripBean> blockTrips = blockInstance
						.getBlockConfiguration().getTrips();
				if (blockTrips == null)
					_log.warn("blockTrips is NULL");
				
				if(blockTrips.size() == 0){
					_log.warn("blockTrips is EMPTY");
				}
				
				boolean match = false;
				for (BlockTripBean blockTrip : blockTrips) {
					if (tripId.equals(blockTrip.getTrip().getId())) {
						match = true;
						for (BlockStopTimeBean bst : blockTrip
								.getBlockStopTimes()) {
							_log.info("BlockStopTime Stop Id: "
									+ bst.getStopTime().getStop().getId());
							_log.info("BlockStopTime Service Date: "
									+ Long.toString(tripStatus.getServiceDate()));
							_log.info("BlockStopTIme Arrival Time:"
									+ Long.toString(bst.getStopTime()
											.getArrivalTime() * 1000));
						}
					}
				}
				
				if(match){
					_log.info("found a match between Trip Update trip ID and blockTrip Trip Id");
				}
				else{
					_log.info("::NO MATCH:: between Trip Update trip ID and blockTrip Trip Id");
				}
				_log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

			}

			// Check if cache contains record
			if (containsId(vehicleId, getVehicleId())) {
				List<TimepointPredictionRecord> records = getCache()
						.getIfPresent(hash(vehicleId, tripId));
				if (records != null && records.size() > 0) {
					_log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					_log.info("Vehicle " + vehicleId + " has " + records.size()
							+ " Time Prediction Records.");
					_log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				} else {
					_log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					_log.warn("Vehicle " + vehicleId
							+ " has no Time Prediction Records.");
					_log.warn("Route ID " + routeId);
					_log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				}
			}
		}
		if (getQueueCount()) {
			processedCount++;
			if (processedCount > _countInterval) {
				long timeInterval = (new Date().getTime() - markTimestamp
						.getTime());
				_log.info(getQueueDisplayName()
						+ " predictions queue: processed " + _countInterval
						+ " messages in " + (timeInterval / 1000)
						+ " seconds. ("
						+ (1000.0 * processedCount / timeInterval)
						+ ") records/second");

				markTimestamp = new Date();
				processedCount = 0;
			}
		}
	}

	private String hash(String vehicleId, String tripId) {
		return vehicleId + "-" + tripId;
	}

	private Map<String, Long> loadScheduledTimes(String vehicleId, String tripId) {
		Map<String, Long> map = new HashMap<String, Long>();
		VehicleStatusBean vehicleStatus = _transitDataService
				.getVehicleForAgency(vehicleId, System.currentTimeMillis());

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

	@Override
	public void updatePredictionsForVehicle(AgencyAndId vehicleId) {
		// no op, messages come in from queue
	}

	@Override
	public List<TimepointPredictionRecord> getPredictionsForTrip(
			TripStatusBean tripStatus) {
		return getCache().getIfPresent(
				hash(tripStatus.getVehicleId(), tripStatus.getActiveTrip()
						.getId()));
	}

	public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(
			String VehicleId, String TripId) {
		return getCache().getIfPresent(hash(VehicleId, TripId));
	}

	private String getTime(Long milliseconds) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliseconds);
		return sdf.format(calendar.getTime());
	}

	private boolean containsId(String idWithPrefix, String configId) {
		int i = idWithPrefix.lastIndexOf('_');
		if (i >= 0) {
			String id = idWithPrefix.substring(i + 1);
			if (id.equals(configId)) {
				return true;
			}
		}
		return false;
	}

	private String getStopId() {
		/*
		 * if(_stopId == null){ refreshLoggingConfig(); }
		 */
		return "981010";
	}

	private String getVehicleId() {
		if (_vehicleId == null) {
			refreshLoggingConfig();
		}
		return _vehicleId;
	}

	private boolean getQueueCount() {
		if (_enableQueueCount == null) {
			refreshLoggingConfig();
		}
		return _enableQueueCount;
	}
}