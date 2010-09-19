package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UpdateVehicleLocationController {

  private VehicleLocationService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @RequestMapping("/update-vehicle-location.do")
  public ModelAndView index(
      @RequestParam() String vehicleId,
      @RequestParam() double lat,
      @RequestParam() double lon,
      @RequestParam() String dsc,
      @RequestParam(required = false, defaultValue = "false") boolean saveResults) {

    _vehicleLocationService.handleVehicleLocation(System.currentTimeMillis(),
        vehicleId, lat, lon, dsc, saveResults);

    return new ModelAndView("update-vehicle-location.jspx");
  }
}
