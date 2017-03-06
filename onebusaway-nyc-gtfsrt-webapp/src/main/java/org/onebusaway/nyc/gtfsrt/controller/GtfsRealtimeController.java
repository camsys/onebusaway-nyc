package org.onebusaway.nyc.gtfsrt.controller;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.onebusaway.nyc.gtfsrt.impl.TripUpdateServiceImpl;
import org.onebusaway.nyc.gtfsrt.impl.VehicleUpdateServiceImpl;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class GtfsRealtimeController {

    private FeedMessageService _vehicleUpdateService;
    private FeedMessageService _tripUpdateService;

    @Autowired
    @Qualifier("vehicleUpdateServiceImpl")
    public void setVehicleUpdateService(FeedMessageService vehicleUpdateService) {
        _vehicleUpdateService = vehicleUpdateService;
    }

    @Autowired
    @Qualifier("tripUpdateServiceImpl")
    public void setTripUpdateService(FeedMessageService tripUpdateService) {
        _tripUpdateService = tripUpdateService;
    }

    @RequestMapping(value = "/vehiclePositions")
    public void getVehiclePositions(HttpServletResponse response,
                                    @RequestParam(value = "debug", defaultValue = "false") boolean debug)
            throws IOException {
        FeedMessage msg = _vehicleUpdateService.getFeedMessage();
        writeFeed(response, msg, debug);
    }

    @RequestMapping(value = "/tripUpdates")
    public void getTripUpdates(HttpServletResponse response,
                                    @RequestParam(value = "debug", defaultValue = "false") boolean debug)
            throws IOException {
        FeedMessage msg = _tripUpdateService.getFeedMessage();
        writeFeed(response, msg, debug);
    }

    private void writeFeed(HttpServletResponse response, FeedMessage msg, boolean debug)
        throws IOException {
        if (debug) {
            response.getWriter().print(msg.toString());
        } else {
            msg.writeTo(response.getOutputStream());
        }
    }

}
