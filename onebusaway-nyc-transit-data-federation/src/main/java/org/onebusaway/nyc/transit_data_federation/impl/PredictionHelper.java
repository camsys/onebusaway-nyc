/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.nyc.transit_data_federation.impl;

import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.trips.TimepointPredictionBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.PredictionHelperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Trivial implementation of the PredictionHelperService.  It returns a single prediction
 * for the tripStatus, or prediction records if available.
 *
 */
@Component
public class PredictionHelper implements PredictionHelperService {

    private static Logger _log = LoggerFactory.getLogger(PredictionHelper.class);
    @Override
    public List<TimepointPredictionRecord> getPredictionRecordsForTrip(String agencyId,
                                                                       TripStatusBean tripStatus) {
        List<TimepointPredictionRecord> records = null;
        _log.info("getPredictionRecordsForTrip called!");
        if (agencyId == null)
            return records;
        if (tripStatus == null)
            return records;
        if (!tripStatus.isPredicted())
            return records;

        records = new ArrayList<TimepointPredictionRecord>();

        List<TimepointPredictionBean> beans = tripStatus.getTimepointPredictions();

        if (beans != null && beans.size() > 0) {
            for (TimepointPredictionBean bean : beans) {
                TimepointPredictionRecord tpr = new TimepointPredictionRecord();

                tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(bean.getTimepointId()));
                tpr.setTimepointScheduledTime(bean.getTimepointScheduledTime());
                tpr.setTimepointPredictedTime(bean.getTimepointPredictedArrivalTime());
                // TODO refactor NYC to support unified changes
//                tpr.setTimepointPredictedArrivalTime(bean.getTimepointPredictedArrivalTime());
//                tpr.setTimepointPredictedDepartureTime(bean.getTimepointPredictedDepartureTime());
//                tpr.setStopSequence(bean.getStopSequence());
//                tpr.setTripId(AgencyAndIdLibrary.convertFromString(bean.getTripId()));

                records.add(tpr);
            }

            return records;
        }

        TimepointPredictionRecord tpr = new TimepointPredictionRecord();
        tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(tripStatus.getNextStop().getId()));
        tpr.setTimepointScheduledTime(tripStatus.getLastUpdateTime() + tripStatus.getNextStopTimeOffset() * 1000);
        tpr.setTimepointPredictedTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));
        // TODO refactor NYC to support unified changes
//        tpr.setTimepointPredictedArrivalTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));
//        tpr.setTimepointPredictedDepartureTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));


        records.add(tpr);
        return records;
    }



    public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String vehicleId,
                                                                                 TripStatusBean tripStatus) {

        List<TimepointPredictionRecord> records = null;
        if (vehicleId == null) {
            _log.debug("rejecting for missing vehicle");
            return records;
        }

        if (tripStatus == null) {
            _log.debug("rejecting for empty tripStatus");
            return records;
        }
        if (!tripStatus.isPredicted()) {
            _log.debug("rejecting for non-predicted tripStatus");
            return records;
        }


        List<TimepointPredictionBean> beans = tripStatus.getTimepointPredictions();
        if (!vehicleId.equals(tripStatus.getVehicleId())) {
            _log.debug("rejecting tripStatus with vehicleId=" + vehicleId + " != " + tripStatus.getVehicleId());
            return records;
        }

        records = new ArrayList<TimepointPredictionRecord>();
        if (beans == null || beans.isEmpty()) {
            _log.debug("can't create a list of TPRs -- nothing to do!");
        }

        if (beans != null && beans.size() > 0) {
            for (TimepointPredictionBean bean : beans) {
                TimepointPredictionRecord tpr = new TimepointPredictionRecord();

                tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(bean.getTimepointId()));
                tpr.setTimepointScheduledTime(bean.getTimepointScheduledTime());
                tpr.setTimepointPredictedTime(bean.getTimepointPredictedArrivalTime());
                // TODO refactor NYC to support unified changes
//                tpr.setTimepointPredictedArrivalTime(bean.getTimepointPredictedArrivalTime());
//                tpr.setTimepointPredictedDepartureTime(bean.getTimepointPredictedDepartureTime());
//                tpr.setStopSequence(bean.getStopSequence());
//                tpr.setTripId(AgencyAndIdLibrary.convertFromString(bean.getTripId()));

                records.add(tpr);
            }

            return records;
        }

        TimepointPredictionRecord tpr = new TimepointPredictionRecord();
        tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(tripStatus.getNextStop().getId()));
        tpr.setTimepointScheduledTime(tripStatus.getLastUpdateTime() + tripStatus.getNextStopTimeOffset() * 1000);
        tpr.setTimepointPredictedTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));
        // TODO refactor NYC to support unified changes
//        tpr.setTimepointPredictedArrivalTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));
//        tpr.setTimepointPredictedDepartureTime((long) (tpr.getTimepointScheduledTime() + tripStatus.getScheduleDeviation()));
        records.add(tpr);
        return records;
    }
}
