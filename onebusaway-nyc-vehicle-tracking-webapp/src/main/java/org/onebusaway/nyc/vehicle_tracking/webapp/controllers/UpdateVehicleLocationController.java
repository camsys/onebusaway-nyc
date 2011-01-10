package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UpdateVehicleLocationController {

  private static SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd' 'HH:mm:ss");

  static {
    _format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
  }

  private VehicleLocationService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @RequestMapping("/update-vehicle-location.do")
  public ModelAndView index(
      @RequestParam(required = false, defaultValue = "") String time,
      @RequestParam() String vehicleId,
      @RequestParam() double lat,
      @RequestParam() double lon,
      @RequestParam() String dsc,
      @RequestParam(required = false, defaultValue = "false") boolean saveResults)
      throws ParseException {

    long t = System.currentTimeMillis();

    if (time != null && ! time.trim().isEmpty()) {
      Date date = _format.parse(time);
      t = date.getTime();
    }

    _vehicleLocationService.handleVehicleLocation(t, vehicleId, lat, lon, dsc,
        saveResults);

    return new ModelAndView("update-vehicle-location.jspx");
  }
}
