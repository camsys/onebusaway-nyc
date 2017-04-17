/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.controller;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.nyc.gtfsrt.util.InferredLocationReader;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.predictions.QueuePredictionIntegrationServiceImpl;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Queue;

/**
 * Manually feed data into TDS for integration tests or otherwise.
 */
@Controller
public class InputController {

    private static final Logger _log = LoggerFactory.getLogger(InputController.class);
    private static final String QUEUE_ADDRESS = "time";

    @Autowired
    private NycTransitDataService _transitDataService;
    @Autowired
    private QueuePredictionIntegrationServiceImpl _predictionService;

    @RequestMapping(value = "/input/vehicleLocationRecords", method = RequestMethod.POST)
    public ResponseEntity addVehicleLocationRecords(@RequestBody String tsv) throws Exception {

        List<VehicleLocationRecordBean> records = new InferredLocationReader().getRecordsFromText(tsv);
        int count = 0;
        for (VehicleLocationRecordBean record : records) {

            _transitDataService.submitVehicleLocation(record);
            VehicleStatusBean status = _transitDataService.getVehicleForAgency(record.getVehicleId(), record.getTimeOfRecord());
            // dump out some message if the status didn't come back as expected
            if (status.getTripStatus() != null && status.getTripStatus().getActiveTrip() != null) {
                _log.warn("status=" + status.getVehicleId() + ":" + status.getTripStatus().getActiveTrip().getId());
                count++;
            } else if (status.getTripStatus() == null){
                _log.warn("status=" + status.getVehicleId() + " has null trip status");
            } else if (status.getTripStatus().getActiveTrip() == null) {
                _log.warn("status=" + status.getVehicleId() + " has no active trip");
            }
        }
        _log.info("" + count + " records processed successfully!");
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/input/timePredictionRecordsTime", method = RequestMethod.POST)
    public ResponseEntity addTimePredictionRecords(@RequestParam String time) throws Exception {
        try {
            _predictionService.setTime(Long.parseLong(time));
        } catch (Exception any) {
            _log.error("issue processing timePredictionRecords:", any);
            throw any;
        }
        _log.info("Time prediction time set");
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/input/timePredictionRecords", method = RequestMethod.POST)
    public ResponseEntity addTimePredictionRecords(@RequestBody byte[] buff) throws Exception {
        try {
            _predictionService.processResult(GtfsRealtime.FeedMessage.parseFrom(buff));
        } catch (Exception any) {
            _log.error("issue processing timePredictionRecords:", any);
            throw any;
        }
        _log.info("Time prediction(s) processed");
        return new ResponseEntity(HttpStatus.OK);
    }
}
