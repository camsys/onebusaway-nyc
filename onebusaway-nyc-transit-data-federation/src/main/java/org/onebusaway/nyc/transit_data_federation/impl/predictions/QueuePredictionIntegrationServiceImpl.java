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
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Listen for (queue), cache, and return time predictions.
 * @author sheldonabrown
 *
 */
public class QueuePredictionIntegrationServiceImpl extends
		TimeQueueListenerTask implements PredictionIntegrationService {

	private static final int DEFAULT_CACHE_TIMEOUT = 2 * 60; // seconds
	private static final String CACHE_TIMEOUT_KEY = "tds.prediction.expiry";
	private static final String PREDICTION_AGE_LIMT = "display.predictionAgeLimit";
	private static final String CHECK_PREDICTION_AGE = "display.checkPredictionAge";
	private static final String CHECK_PREDICTION_LATENCY = "display.checkPredictionLatency";
	private static final String DEFAULT_FALSE = "false";

	@Autowired
	private NycTransitDataService _transitDataService;

	@Autowired
	private ConfigurationService _configurationService;


	private Cache<String, List<TimepointPredictionRecord>> _cache = null;

	private Boolean _checkPredictionAge;
	private Boolean _checkPredictionLatency;
	private Integer _predictionAgeLimit = 300;
	private int predictionRecordCount = 0;
	private int predictionRecordCountInterval = 2000;
	private long predictionRecordAverageLatency = 0;


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
		if (enableCheckPredictionAge()) {
			logPredictionLatency(message);
		}
		if (enableCheckPredictionAge() &&
				isPredictionRecordPastAgeLimit(message)) {
			// Drop old messages
			return;
		}

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
//				tpr.setStopSequence(stu.getStopSequence()); // 0-indexed stop sequence
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
		//TripBean trip = _transitDataService.getTrip(tripId);
		TripDetailsQueryBean tqb = new TripDetailsQueryBean();
		tqb.setTripId(tripId);
		tqb.setTime(System.currentTimeMillis());
		tqb.setVehicleId(vehicleId);
		tqb.setInclusion(new TripDetailsInclusionBean(false, true, true));
		ListBean<TripDetailsBean> tripDetails = _transitDataService.getTripDetails(tqb);
		Long serviceDate = null;
		if (tripDetails.getList().get(0).getStatus() != null) {
			serviceDate = tripDetails.getList().get(0).getStatus().getServiceDate();
		} else {
			// TDS doesn't know of trip status, no need to store prediction
			return map;
		}
		assert(tripDetails.getList().size() == 1);
		TripStopTimesBean schedule = tripDetails.getList().get(0).getSchedule();

		for (TripStopTimeBean stopTime : schedule.getStopTimes()) {

			map.put(stopTime.getStop().getId(),
					serviceDate + stopTime.getArrivalTime() * 1000);
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

	@Override
	public boolean isEnabled() {
		return !"true".equalsIgnoreCase(getDisable());
	}

	public void destroy() {
		super.destroy();
		_log.warn("destroy called!");
	}
	private boolean enableCheckPredictionLatency() {
		if (_checkPredictionLatency == null) {
            refreshConfig();
		}
		return _checkPredictionLatency;
	}

	private void logPredictionLatency(FeedMessage message){
		long currentTime = System.currentTimeMillis();
		Long messageTimeStamp = message.getHeader().getTimestamp();
		if (messageTimeStamp != null && messageTimeStamp > 0) {
			predictionRecordCount++;
			predictionRecordAverageLatency += (currentTime - messageTimeStamp);
			if (predictionRecordCount >= predictionRecordCountInterval) {
				String avgPredictionRecordLatencyAsText = getHumanReadableElapsedTime(predictionRecordAverageLatency
						/ predictionRecordCountInterval);
				_log.info("Average predictions message latency for predictions is: "
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
		return (System.currentTimeMillis() - timestamp) / 1000; // output in seconds
	}
}