package org.onebusaway.nyc.gtfsrt.controller;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.onebusaway.nyc.gtfsrt.impl.VehicleUpdateServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class GtfsRealtimeController {

    private VehicleUpdateServiceImpl _vehicleUpdateService;

    @Autowired
    public void setVehicleUpdateService(VehicleUpdateServiceImpl vehicleUpdateService) {
        _vehicleUpdateService = vehicleUpdateService;
    }

    @RequestMapping(value = "/vehiclePositions")
    public void getVehiclePositions(HttpServletResponse response,
                                    @RequestParam(value = "debug", defaultValue = "false") boolean debug)
            throws IOException {
        FeedMessage msg = _vehicleUpdateService.getFeedMessage();
        if (debug) {
            response.getWriter().print(msg.toString());
        } else {
            msg.writeTo(response.getOutputStream());
        }
    }

}
