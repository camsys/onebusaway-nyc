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

import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.beans.BlockBeanService;
import org.onebusaway.transit_data_federation.services.beans.BlockStatusBeanService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class VehicleLocationSimulationController {

  private static final Pattern CALENDAR_OFFSET_PATTERN = Pattern.compile("^(-{0,1}\\d+)(\\w+)$");

  private static final String CALENDAR_OFFSET_KEY = VehicleLocationSimulationController.class
      + ".calendarOffset";

  private VehicleLocationSimulationService _vehicleLocationSimulationService;
  
  private TransitDataService _transitDataService;
  
  private VehicleLocationInferenceService _vehicleLocationInferenceService;

  private AgencyService _agencyService;

  private BlockBeanService _blockBeanService;

  private BlockStatusBeanService _blockStatusBeanService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationInferenceService = vehicleLocationService;
  }

  @Autowired
  public void setTransitDataService(
      TransitDataService transitDataService) {
    _transitDataService = transitDataService;
  }

  @Autowired
  public void setVehicleLocationSimulationService(
      VehicleLocationSimulationService service) {
    _vehicleLocationSimulationService = service;
  }

  @Autowired
  public void setBlockBeanService(BlockBeanService blockBeanService) {
    _blockBeanService = blockBeanService;
  }

  @Autowired
  public void setAgencyService(AgencyService agencyService) {
    _agencyService = agencyService;
  }

  @Autowired
  public void setBlockStatusBeanService(
      BlockStatusBeanService blockStatusBeanService) {
    _blockStatusBeanService = blockStatusBeanService;
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
    dateFormat.setLenient(false);
    binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat,
        false));
  }

  @RequestMapping(value = "/vehicle-location-simulation.do", method = RequestMethod.GET)
  public ModelAndView index() {
    List<VehicleLocationSimulationSummary> simulations = _vehicleLocationSimulationService.getSimulations();
    return new ModelAndView("vehicle-location-simulation.jspx", "simulations",
        simulations);
  }

  @RequestMapping(value = "/vehicle-location-simulation!set-calendar-offset.do", method = RequestMethod.GET)
  public ModelAndView setCalendarOffser(HttpSession session,
      @RequestParam(required = false) String calendarOffset) {
    if (calendarOffset != null)
      session.setAttribute(CALENDAR_OFFSET_KEY, calendarOffset);
    else
      session.removeAttribute(CALENDAR_OFFSET_KEY);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }
  
  // for integration testing
  @RequestMapping(value = "/vehicle-location-simulation!set-seeds.do", method = RequestMethod.GET)
  public ModelAndView setSeeds(HttpSession session,
      @RequestParam(value = "cdfSeed", required = false, defaultValue = "0") long cdfSeed,
      @RequestParam(value = "phaseSeed", required = false, defaultValue = "0") long phaseSeed) {
    if (cdfSeed != 0) {
      _vehicleLocationInferenceService.setCdfSeed(cdfSeed);
    }
    
    if (phaseSeed != 0) {
      _vehicleLocationInferenceService.setPhaseSeed(phaseSeed);
    }

    return new ModelAndView("vehicle-location-simulation-set-seeds.jspx");
  }

  @RequestMapping(value = "/vehicle-location-simulation!upload-trace.do", method = RequestMethod.POST)
  public ModelAndView uploadTrace(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "realtime", required = false, defaultValue = "false") boolean realtime,
      @RequestParam(value = "pauseOnStart", required = false, defaultValue = "false") boolean pauseOnStart,
      @RequestParam(value = "shiftStartTime", required = false, defaultValue = "false") boolean shiftStartTime,
      @RequestParam(value = "minimumRecordInterval", required = false, defaultValue = "0") int minimumRecordInterval,
      @RequestParam(value = "traceType", required = true) String traceType,
      @RequestParam(required = false, defaultValue = "false") boolean bypassInference,
      @RequestParam(required = false, defaultValue = "false") boolean fillActualProperties,
      @RequestParam(value = "loop", required = false, defaultValue = "false") boolean loop,
      @RequestParam(required = false, defaultValue = "false") boolean returnId)
      throws IOException {

    int taskId = -1;
    
    if (!file.isEmpty()) {

      String name = file.getOriginalFilename();
      InputStream in = file.getInputStream();

      if (name.endsWith(".gz"))
        in = new GZIPInputStream(in);

      taskId = _vehicleLocationSimulationService.simulateLocationsFromTrace(
          traceType, in, realtime, pauseOnStart, shiftStartTime,
          minimumRecordInterval, bypassInference, fillActualProperties, loop);
    }

    if (returnId) {
      return new ModelAndView("vehicle-location-simulation-taskId.jspx",
          "taskId", taskId);
    } else {
      return new ModelAndView("redirect:/vehicle-location-simulation.do");
    }
  }

  @RequestMapping(value = "/vehicle-location-simulation!toggle.do", method = RequestMethod.GET)
  public ModelAndView toggle(@RequestParam() int taskId) {
    _vehicleLocationSimulationService.toggleSimulation(taskId);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!step.do", method = RequestMethod.GET)
  public ModelAndView step(@RequestParam() int taskId) {
    _vehicleLocationSimulationService.stepSimulation(taskId);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!restart.do", method = RequestMethod.GET)
  public ModelAndView restart(@RequestParam() int taskId) {
    _vehicleLocationSimulationService.restartSimulation(taskId);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!step-to.do", method = RequestMethod.GET)
  public ModelAndView stepTo(@RequestParam() int taskId,
      @RequestParam() int recordIndex) {
    _vehicleLocationSimulationService.stepSimulation(taskId, recordIndex);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!cancel.do", method = RequestMethod.GET)
  public ModelAndView cancel(@RequestParam() int taskId) {
    _vehicleLocationSimulationService.cancelSimulation(taskId);
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!cancelAll.do", method = RequestMethod.GET)
  public ModelAndView cancelAll() {
    _vehicleLocationSimulationService.cancelAllSimulations();
    return new ModelAndView("redirect:/vehicle-location-simulation.do");
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-details.do", method = RequestMethod.GET)
  public ModelAndView taskDetails(
      @RequestParam() int taskId,
      @RequestParam(required = false, defaultValue = "0") int historyOffset,
      @RequestParam(required = false, defaultValue = "false") boolean showSampledParticles) {

    VehicleLocationDetails details = _vehicleLocationSimulationService.getSimulationDetails(
        taskId, historyOffset);
    Map<String, Object> m = new HashMap<String, Object>();
    m.put("details", details);
    m.put("historyOffset", historyOffset);
    m.put("showSampledParticles", showSampledParticles);
    return new ModelAndView("vehicle-location-simulation-task-details.jspx", m);
  }

  @RequestMapping(value = "/vehicle-location-simulation!particle-details.do", method = RequestMethod.GET)
  public ModelAndView particleDetails(@RequestParam() int taskId,
      @RequestParam() int particleId) {

    VehicleLocationDetails details = _vehicleLocationSimulationService.getParticleDetails(
        taskId, particleId);
    return new ModelAndView("vehicle-location-simulation-task-details.jspx",
        "details", details);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-simulation-records.do", method = RequestMethod.GET)
  public void taskRecords(@RequestParam() int taskId,
      HttpServletResponse response) throws IOException {

    List<NycTestInferredLocationRecord> records = _vehicleLocationSimulationService.getSimulationRecords(taskId);
    writeRecordsToOutput(response, records);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-result-records.do", method = RequestMethod.GET)
  public void taskParticles(@RequestParam() int taskId,
      HttpServletResponse response) throws IOException {

    List<NycTestInferredLocationRecord> records = _vehicleLocationSimulationService.getResultRecords(taskId);
    writeRecordsToOutput(response, records);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-result-records-json.do", method = RequestMethod.GET)
  public ModelAndView taskRecordsJson(@RequestParam() int taskId) throws IOException {

    List<NycTestInferredLocationRecord> records = _vehicleLocationSimulationService.getResultRecords(taskId);
    return new ModelAndView("json", "records", records);
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!active-blocks.do", method = RequestMethod.GET)
  public ModelAndView activeBlocks(HttpSession session,
      @RequestParam(required = false) Date time) {

    List<BlockStatusBean> blocks = new ArrayList<BlockStatusBean>();

    time = getTime(session, time);

    for (String agencyId : _agencyService.getAllAgencyIds()) {
      ListBean<BlockStatusBean> beans = _blockStatusBeanService.getBlocksForAgency(
          agencyId, time.getTime());
      blocks.addAll(beans.getList());
    }

    Collections.sort(blocks, new BlockStatusBeanComparator());

    Map<String, Object> m = new HashMap<String, Object>();
    m.put("blocks", blocks);
    m.put("time", time);

    return new ModelAndView("vehicle-location-simulation-active-blocks.jspx", m);
  }

  @RequestMapping(value = "/vehicle-location-simulation!active-routes-json.do", method = RequestMethod.GET)
  public ModelAndView activeRoutesJson(HttpSession session,
      @RequestParam(required = false) Date time) {

    time = getTime(session, time);

    List<String> routeList = new ArrayList<String>();    
    for (String agencyId : _agencyService.getAllAgencyIds()) {
      ListBean<RouteBean> routes = _transitDataService.getRoutesForAgencyId(agencyId);
      for(RouteBean route : routes.getList()) {
        routeList.add(route.getId());
      }
    }
   
    Collections.sort(routeList);
    
    return new ModelAndView("json", "routes", routeList);
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!active-blocks-for-route-json.do", method = RequestMethod.GET)
  public ModelAndView activeBlocksForRouteJson(@RequestParam(required=true) String routeId) {
    Map<String, ArrayList<String>> m = new HashMap<String, ArrayList<String>>();

    ListBean<BlockStatusBean> beans = _blockStatusBeanService.getBlocksForRoute(
          AgencyAndIdLibrary.convertFromString(routeId), System.currentTimeMillis());

    for(BlockStatusBean blockBean : beans.getList()) {
      BlockBean block = blockBean.getBlock();
        
      ArrayList<String> trips = new ArrayList<String>();
      for(BlockConfigurationBean config : block.getConfigurations()) {
        for(BlockTripBean blockTrip : config.getTrips()) {
          trips.add(blockTrip.getTrip().getId());
        }
      }

      m.put(block.getId(), trips);
    }

    return new ModelAndView("json", "blocks", m);
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!points-for-trip-id.do", method = RequestMethod.GET)
  public ModelAndView pointsForTripJson(@RequestParam(required=true) String tripId) {
    TripBean trip = _transitDataService.getTrip(tripId);
    EncodedPolylineBean polyline = _transitDataService.getShapeForId(trip.getShapeId());
    
    return new ModelAndView("json", "points", polyline.getPoints());
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!stops-for-trip-id.do", method = RequestMethod.GET)
  public ModelAndView stopsForTripJson(@RequestParam(required=true) String tripId) {
    
    TripDetailsQueryBean query = new TripDetailsQueryBean();
    query.setTripId(tripId);
    TripDetailsBean trip = _transitDataService.getSingleTripDetails(query);
    TripStopTimesBean stops = trip.getSchedule();
    
    return new ModelAndView("json", "stopTimes", stops.getStopTimes());
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!map.do", method = RequestMethod.GET)
  public ModelAndView activeBlocksAndRecordsForTask(@RequestParam() int taskId,
      HttpServletResponse response) throws IOException {

    return new ModelAndView("vehicle-location-simulation-map.jspx", null);
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!block.do", method = RequestMethod.GET)
  public ModelAndView block(@RequestParam String blockId,
      @RequestParam long serviceDate) {

    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    BlockBean block = _blockBeanService.getBlockForId(id);

    List<VehicleLocationSimulationSummary> simulations = _vehicleLocationSimulationService.getSimulationsForBlockInstance(
        id, serviceDate);

    Map<String, Object> model = new HashMap<String, Object>();
    model.put("block", block);
    model.put("serviceDate", serviceDate);
    model.put("simulations", simulations);
    return new ModelAndView("vehicle-location-simulation-block.jspx", model);
  }

  @RequestMapping(value = "/vehicle-location-simulation!block-add-simulation.do", method = RequestMethod.POST)
  public ModelAndView addBlockSimulation(
      HttpSession session,
      @RequestParam String blockId,
      @RequestParam long serviceDate,
      @RequestParam(required = false, defaultValue = "false") boolean bypassInference,
      @RequestParam(value = "isRunDriven", required = false, defaultValue = "false") boolean isRunDriven,
      @RequestParam(value = "realtime", required = false, defaultValue = "false") boolean realtime,
      @RequestParam(value = "reportsOperatorId", required = false, defaultValue = "false") boolean reportsOperatorId,
      @RequestParam(value = "reportsRunId", required = false, defaultValue = "false") boolean reportsRunId,
      @RequestParam(value = "allowRunTransitions", required = false, defaultValue = "false") boolean allowRunTransitions,
      @RequestParam(value = "fillActual", required = false, defaultValue = "false") boolean fillActualProperties,
      @RequestParam String properties) throws IOException {

    Date time = getTime(session, null);

    Properties props = new Properties();
    props.load(new StringReader(properties));

    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);

    _vehicleLocationSimulationService.addSimulationForBlockInstance(id,
        serviceDate, time.getTime(), bypassInference, isRunDriven, 
        realtime, fillActualProperties, reportsOperatorId, reportsRunId, 
        allowRunTransitions, props);

    Map<String, Object> model = new HashMap<String, Object>();
    model.put("blockId", blockId);
    model.put("serviceDate", serviceDate);
    return new ModelAndView("redirect:/vehicle-location-simulation!block.do",
        model);
  }

  /****
   * Private Methods
   ****/

  private void writeRecordsToOutput(HttpServletResponse response,
      List<NycTestInferredLocationRecord> records) throws IOException {

    CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
    OutputStreamWriter writer = new OutputStreamWriter(
        response.getOutputStream());

    EntityHandler handler = factory.createWriter(NycTestInferredLocationRecord.class,
        writer);

    if (records == null)
      records = Collections.emptyList();

    for (NycTestInferredLocationRecord record : records)
      handler.handleEntity(record);

    writer.close();
  }

  private Date getTime(HttpSession session, Date time) {

    if (time == null)
      time = new Date();

    String calendarOffset = (String) session.getAttribute(CALENDAR_OFFSET_KEY);

    if (calendarOffset != null) {

      Matcher m = CALENDAR_OFFSET_PATTERN.matcher(calendarOffset);
      if (m.matches()) {

        int amount = Integer.parseInt(m.group(1));
        String type = m.group(2);

        Calendar c = Calendar.getInstance();
        c.setTime(time);

        if (type.equals("M"))
          c.add(Calendar.MONTH, amount);
        else if (type.equals("w"))
          c.add(Calendar.WEEK_OF_YEAR, amount);
        else if (type.equals("d"))
          c.add(Calendar.DAY_OF_YEAR, amount);
        else if (type.equals("h"))
          c.add(Calendar.HOUR, amount);
        time = c.getTime();
      }
    }

    return time;
  }

  private static class BlockStatusBeanComparator implements
      Comparator<BlockStatusBean> {

    @Override
    public int compare(BlockStatusBean o1, BlockStatusBean o2) {

      String r1 = getRouteShortName(o1);
      String r2 = getRouteShortName(o2);

      int rc = r1.compareTo(r2);
      if (rc != 0)
        return rc;

      String headsign1 = getTripHeadsign(o1);
      String headsign2 = getTripHeadsign(o2);

      rc = headsign1.compareTo(headsign2);
      if (rc != 0)
        return rc;

      double d1 = o1.computeBestDistanceAlongBlock();
      double d2 = o2.computeBestDistanceAlongBlock();

      return Double.compare(d1, d2);
    }

    private String getRouteShortName(BlockStatusBean bean) {
      BlockTripBean activeTrip = bean.getActiveTrip();
      TripBean trip = activeTrip.getTrip();
      if (trip.getRouteShortName() != null)
        return trip.getRouteShortName();
      RouteBean route = trip.getRoute();
      return route.getShortName();
    }

    private String getTripHeadsign(BlockStatusBean bean) {
      BlockTripBean activeTrip = bean.getActiveTrip();
      TripBean trip = activeTrip.getTrip();
      if (trip.getTripHeadsign() != null)
        return trip.getTripHeadsign();
      RouteBean route = trip.getRoute();
      return route.getLongName();
    }
  }
}
