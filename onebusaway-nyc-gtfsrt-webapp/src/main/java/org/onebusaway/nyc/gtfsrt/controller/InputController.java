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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
    public ModelAndView addVehicleLocationRecords(@RequestBody String tsv) throws Exception {

        List<VehicleLocationRecordBean> records = new InferredLocationReader().getRecordsFromText(tsv);
        int count = 0;
        for (VehicleLocationRecordBean record : records) {
            count++;
            _transitDataService.submitVehicleLocation(record);
            VehicleStatusBean status = _transitDataService.getVehicleForAgency(record.getVehicleId(), record.getTimeOfRecord());
        }
        _log.info("" + count + " records processed!");
        // todo this is just general success
        return new ModelAndView("redirect:/bundles-change.jspx");
    }

    @RequestMapping(value = "/input/timePredictionRecords", method = RequestMethod.POST)
    public ModelAndView addTimePredictionRecords(@RequestBody byte[] buff) throws Exception {
        boolean success = _predictionService.processMessage(QUEUE_ADDRESS, buff);
        _log.info("Time prediction(s) processed? " + success);
        // todo this is just general success
        return new ModelAndView("redirect:/bundles-change.jspx");
    }
}
