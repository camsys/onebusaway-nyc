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

import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycQueuedInferredLocationBeanVehicleComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycTestInferredLocationRecordDestinationSignCodeComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.NycTestInferredLocationRecordVehicleComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class VehicleLocationsController {

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @Autowired
  private DestinationSignCodeService _dscService;

  @Autowired
  private VehicleLocationInferenceService _vehicleLocationInferenceService;

  @RequestMapping("/vehicle-locations.do")
  public ModelAndView index(@RequestParam(required = false) String sort) {

    List<NycTestInferredLocationRecord> records = _vehicleLocationInferenceService.getLatestProcessedVehicleLocationRecords();

    Comparator<NycTestInferredLocationRecord> comparator = getComparatorForSortString(sort);
    Collections.sort(records, comparator);

    ModelAndView mv = new ModelAndView("vehicle-locations.jspx");
    mv.addObject("records", records);
    return mv;
  }
  
  @RequestMapping("/queued-vehicle-locations.do")
  public ModelAndView queuedVehicleLocations() {

    List<NycQueuedInferredLocationBean> records = _vehicleLocationInferenceService.getLatestProcessedQueuedVehicleLocationRecords();

    Collections.sort(records, new NycQueuedInferredLocationBeanVehicleComparator());

    ModelAndView mv = new ModelAndView("queued-vehicle-locations.jspx");
    mv.addObject("records", records);
    return mv;
  }

  @RequestMapping("/vehicle-location.do")
  public ModelAndView vehicleLocation(@RequestParam() String vehicleId) {
    NycTestInferredLocationRecord record = _vehicleLocationInferenceService.getNycTestInferredLocationRecordForVehicle(AgencyAndIdLibrary.convertFromString(vehicleId));
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

  @RequestMapping(value = "/vehicle-location!map.do", method = RequestMethod.GET)
  public ModelAndView map(@RequestParam() Double lat, @RequestParam() Double lon, @RequestParam() String dsc) {
    return new ModelAndView("vehicle-location-map.jspx");
  }
  
  @RequestMapping(value = "/vehicle-location!lines-for-dsc-json.do", method = RequestMethod.GET)
  public ModelAndView linesForDSC(@RequestParam() String dsc) throws IOException {
    Map<String, List<String>> routes = new HashMap<String, List<String>>();

    List<AgencyAndId> trips = _dscService.getTripIdsForDestinationSignCode(dsc, null);    
    for(AgencyAndId tripId : trips) {
      TripBean tripBean = _nycTransitDataService.getTrip(tripId.toString());
      String routeId = tripBean.getRoute().getId();
      
      if(routes.get(routeId) == null) {
        StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeId);
        List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
        for (StopGroupingBean stopGroupingBean : stopGroupings) {
          for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
            NameBean name = stopGroupBean.getName();
            String type = name.getType();
            if (!type.equals("destination"))
              continue;

            List<String> polylinesAsString = new ArrayList<String>();

            List<EncodedPolylineBean> polylines = stopGroupBean.getPolylines();
            for(EncodedPolylineBean epb : polylines) {
              polylinesAsString.add(epb.getPoints());
            }
            
            routes.put(routeId, polylinesAsString);
          }
        }
      }
    }
    
    return new ModelAndView("json", "polylinesByRoute", routes);
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
  private Comparator<NycTestInferredLocationRecord> getComparatorForSortString(
      String sort) {
    if (sort == null)
      return new NycTestInferredLocationRecordVehicleComparator();

    sort = sort.toLowerCase().trim();

    if (sort.equals("dsc"))
      return new NycTestInferredLocationRecordDestinationSignCodeComparator();
    


    return new NycTestInferredLocationRecordVehicleComparator();
  }
}  