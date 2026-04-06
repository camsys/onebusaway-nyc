/**
 * Copyright (C) 2018 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * For testing: PredictionIntegrationService that just uses scheduled times as the "predictions"
 */
public class ScheduleBasedPredictionIntegrationServiceImpl implements PredictionIntegrationService {

    @Autowired
    @Qualifier("nycTransitDataServiceImpl")
    private TransitDataService _transitDataService;

    @Override
    public void updatePredictionsForVehicle(AgencyAndId vehicleId) {
        // no op
    }

    @Override
    public Collection<TimepointPredictionRecord> getPredictionsForTrip(TripStatusBean tripStatus) {
        String tripId = tripStatus.getActiveTrip().getId();
        return getPredictionsForTrip(tripId);
    }

    @Override
    public Map<String, TimepointPredictionRecord> getPredictionsForTripByStopId(TripStatusBean tripStatus) {
        String tripId = tripStatus.getActiveTrip().getId();
        return getPredictionsForTripByStopId(tripId);
    }

    @Override
    public Collection<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String VehicleId, String TripId) {
        return getPredictionsForTrip(TripId);
    }

    @Override
    public Map<String, TimepointPredictionRecord> getPredictionRecordsForVehicleAndTripByStopId(String vehicleId, String tripId) {
        return getPredictionsForTripByStopId(tripId);
    }

    private Collection<TimepointPredictionRecord> getPredictionsForTrip(String tripId) {
        TripDetailsQueryBean query = new TripDetailsQueryBean();
        query.setTripId(tripId);
        query.setTime(new Date().getTime()); // time in ms
        ListBean<TripDetailsBean> response = _transitDataService.getTripDetails(query);
        if (response.getList().isEmpty()) {
            return Collections.emptyList();
        }
        TripDetailsBean tripDetails = response.getList().iterator().next();
        Collection<TimepointPredictionRecord> predictions = new ArrayList<TimepointPredictionRecord>();
        for(TripStopTimeBean stopTime : tripDetails.getSchedule().getStopTimes()) {
            TimepointPredictionRecord tpr = new TimepointPredictionRecord();
            tpr.setStopSequence(stopTime.getGtfsSequence());
            tpr.setTimepointId(AgencyAndId.convertFromString(stopTime.getStop().getId()));
            // tpr time is milliseconds unix epoch time
            long arrivalTime = tripDetails.getServiceDate() + (stopTime.getArrivalTime() * 1000);
            long departureTime = tripDetails.getServiceDate() + (stopTime.getDepartureTime() * 1000);

            tpr.setTimepointPredictedArrivalTime(arrivalTime);
            tpr.setTimepointPredictedDepartureTime(departureTime);
            tpr.setTimepointScheduledTime(stopTime.getArrivalTime());
            predictions.add(tpr);
        }
        return predictions;
    }

    private Map<String, TimepointPredictionRecord> getPredictionsForTripByStopId(String tripId) {
        Collection<TimepointPredictionRecord> predictionsForTrip = getPredictionsForTrip(tripId);
        return predictionsForTrip.stream().collect(Collectors.toMap(
                record -> record.getTimepointId().toString(),
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
