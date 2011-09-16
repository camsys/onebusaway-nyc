/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycInferredLocationRecordDestinationSignCodeComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycInferredLocationRecordVehicleComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VehicleLocationsController {

  private VehicleLocationInferenceService _vehicleLocationInferenceService;

  @Autowired
  public void setVehicleLocationService(
		  VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationInferenceService = vehicleLocationService;
  }

  @RequestMapping("/vehicle-locations.do")
  public ModelAndView index(@RequestParam(required = false) String sort) {

    List<NycInferredLocationRecord> records = _vehicleLocationInferenceService.getLatestProcessedVehicleLocationRecords();

    Comparator<NycInferredLocationRecord> comparator = getComparatorForSortString(sort);
    Collections.sort(records, comparator);

    ModelAndView mv = new ModelAndView("vehicle-locations.jspx");
    mv.addObject("records", records);
    return mv;
  }

  @RequestMapping("/vehicle-location.do")
  public ModelAndView vehicleLocation(@RequestParam() String vehicleId) {
    NycInferredLocationRecord record = _vehicleLocationInferenceService.getVehicleLocationForVehicle(AgencyAndIdLibrary.convertFromString(vehicleId));
    return new ModelAndView("json", "record", record);
  }

  @RequestMapping("/vehicle-location!reset.do")
  public ModelAndView resetVehicleLocation(@RequestParam() String vehicleId) {
    _vehicleLocationInferenceService.resetVehicleLocation(AgencyAndIdLibrary.convertFromString(vehicleId));
    return new ModelAndView("redirect:/vehicle-locations.do");
  }

  @RequestMapping("/vehicle-location!particles.do")
  public ModelAndView particles(@RequestParam() String vehicleId) {

    VehicleLocationDetails details = _vehicleLocationInferenceService.getDetailsForVehicleId(AgencyAndIdLibrary.convertFromString(vehicleId));

    ModelAndView mv = new ModelAndView("vehicle-location-particles.jspx");
    mv.addObject("details", details);
    return mv;
  }

  @RequestMapping(value = "/vehicle-location!particle-details.do")
  public ModelAndView particleDetails(@RequestParam() String vehicleId,
      @RequestParam() int particleId) {

    VehicleLocationDetails details = _vehicleLocationInferenceService.getDetailsForVehicleId(
    	AgencyAndIdLibrary.convertFromString(vehicleId), particleId);

    return new ModelAndView("vehicle-location-particles.jspx", "details",
        details);
  }

  @RequestMapping("/vehicle-location!bad-particles.do")
  public ModelAndView badParticles(@RequestParam() String vehicleId) {

    VehicleLocationDetails details = _vehicleLocationInferenceService.getBadDetailsForVehicleId(AgencyAndIdLibrary.convertFromString(vehicleId));

    return new ModelAndView("vehicle-location-particles.jspx", "details",
        details);
  }

  @RequestMapping(value = "/vehicle-location!bad-particle-details.do")
  public ModelAndView badParticleDetails(@RequestParam() AgencyAndId vehicleId,
      @RequestParam() int particleId) {

    VehicleLocationDetails details = _vehicleLocationInferenceService.getBadDetailsForVehicleId(
    	vehicleId, particleId);

    return new ModelAndView("vehicle-location-particles.jspx", "details",
        details);
  }

  @RequestMapping("/vehicle-location!particle.do")
  public ModelAndView particles(@RequestParam() String vehicleId,
      @RequestParam() int particleId) {

    VehicleLocationDetails details = _vehicleLocationInferenceService.getDetailsForVehicleId(
    	AgencyAndIdLibrary.convertFromString(vehicleId), particleId);

    ModelAndView mv = new ModelAndView("vehicle-location-particles.jspx");
    mv.addObject("details", details);
    return mv;
  }

  /****
   * 
   ****/
  private Comparator<NycInferredLocationRecord> getComparatorForSortString(
      String sort) {
    if (sort == null)
      return new NycInferredLocationRecordVehicleComparator();

    sort = sort.toLowerCase().trim();

    if (sort.equals("dsc"))
      return new NycInferredLocationRecordDestinationSignCodeComparator();

    return new NycInferredLocationRecordVehicleComparator();
  }
}
