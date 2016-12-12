package org.onebusaway.nyc.transit_data_federation.services.predictions;

import java.util.List;

import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import com.google.common.cache.Cache;

public interface PredictionCacheService {
	Cache<String, List<TimepointPredictionRecord>> getCache();

	List<TimepointPredictionRecord> getPredictionsForTrip(
			TripStatusBean tripStatus);

	List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(
			String VehicleId, String TripId);
}
