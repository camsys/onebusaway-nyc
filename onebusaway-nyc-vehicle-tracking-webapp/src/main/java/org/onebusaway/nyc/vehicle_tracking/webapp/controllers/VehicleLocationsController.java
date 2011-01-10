package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycTestLocationRecordDestinationSignCodeComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycTestLocationRecordVehicleComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VehicleLocationsController {

  private VehicleLocationService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @RequestMapping("/vehicle-locations.do")
  public ModelAndView index(@RequestParam(required=false) String sort) {

    List<NycTestLocationRecord> records = _vehicleLocationService.getLatestProcessedVehicleLocationRecords();

    Comparator<NycTestLocationRecord> comparator = getComparatorForSortString(sort);
    Collections.sort(records,comparator);
    
    ModelAndView mv = new ModelAndView("vehicle-locations.jspx");
    mv.addObject("records", records);
    return mv;
  }

  @RequestMapping("/vehicle-location.do")
  public ModelAndView vehicleLocation(@RequestParam() String vehicleId) {
    NycTestLocationRecord record = _vehicleLocationService.getVehicleLocationForVehicle(vehicleId);
    return new ModelAndView("json", "record", record);
  }

  @RequestMapping("/vehicle-location!reset.do")
  public ModelAndView resetVehicleLocation(@RequestParam() String vehicleId) {
    _vehicleLocationService.resetVehicleLocation(vehicleId);
    return new ModelAndView("redirect:/vehicle-locations.do");
  }

  @RequestMapping("/vehicle-location!particles.do")
  public ModelAndView particles(@RequestParam() String vehicleId) {

    VehicleLocationDetails details = _vehicleLocationService.getDetailsForVehicleId(vehicleId);

    ModelAndView mv = new ModelAndView("vehicle-location-particles.jspx");
    mv.addObject("details", details);
    return mv;
  }

  @RequestMapping("/vehicle-location!particle.do")
  public ModelAndView particles(@RequestParam() String vehicleId,
      @RequestParam() int particleId) {

    VehicleLocationDetails details = _vehicleLocationService.getParticleDetails(
        vehicleId, particleId);

    ModelAndView mv = new ModelAndView("vehicle-location-particles.jspx");
    mv.addObject("details", details);
    return mv;
  }

  /****
   * 
   ****/

  private Comparator<NycTestLocationRecord> getComparatorForSortString(
      String sort) {
    if( sort == null )
      return new NycTestLocationRecordVehicleComparator();
    
    sort = sort.toLowerCase().trim();
    
    if( sort.equals("dsc"))
      return new NycTestLocationRecordDestinationSignCodeComparator();
    
    return new NycTestLocationRecordVehicleComparator();
  }
}
