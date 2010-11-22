package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

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

import org.onebusaway.gtfs.csv.CsvEntityWriterFactory;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
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

@Controller
public class VehicleLocationSimulationController {

  private static final Pattern CALENDAR_OFFSET_PATTERN = Pattern.compile("^(-{0,1}\\d+)(\\w+)$");

  private static final String CALENDAR_OFFSET_KEY = VehicleLocationSimulationController.class
      + ".calendarOffset";

  private VehicleLocationSimulationService _vehicleLocationSimulationService;

  private AgencyService _agencyService;

  private BlockBeanService _blockBeanService;

  private BlockStatusBeanService _blockStatusBeanService;

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

  @RequestMapping(value = "/vehicle-location-simulation!upload-trace.do", method = RequestMethod.POST)
  public ModelAndView uploadTrace(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "realtime", required = false, defaultValue = "false") boolean realtime,
      @RequestParam(value = "pauseOnStart", required = false, defaultValue = "false") boolean pauseOnStart,
      @RequestParam(value = "shiftStartTime", required = false, defaultValue = "false") boolean shiftStartTime,
      @RequestParam(value = "minimumRecordInterval", required = false, defaultValue="0") int minimumRecordInterval,
      @RequestParam(value = "traceType", required = true) String traceType,
      @RequestParam(required = false, defaultValue = "false") boolean bypassInference,
      @RequestParam(required = false, defaultValue = "false") boolean returnId)
      throws IOException {

    int taskId = -1;

    if (!file.isEmpty()) {

      String name = file.getOriginalFilename();
      InputStream in = file.getInputStream();

      if (name.endsWith(".gz"))
        in = new GZIPInputStream(in);

      taskId = _vehicleLocationSimulationService.simulateLocationsFromTrace(traceType,
          in, realtime, pauseOnStart, shiftStartTime,
          minimumRecordInterval, bypassInference);
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

  @RequestMapping(value = "/vehicle-location-simulation!task-summary.do", method = RequestMethod.GET)
  public ModelAndView taskData(@RequestParam() int taskId) {

    VehicleLocationSimulationSummary summary = _vehicleLocationSimulationService.getSimulation(taskId);
    return new ModelAndView("json", "summary", summary);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-details.do", method = RequestMethod.GET)
  public ModelAndView taskDetails(@RequestParam() int taskId) {

    VehicleLocationSimulationDetails details = _vehicleLocationSimulationService.getSimulationDetails(taskId);
    return new ModelAndView("vehicle-location-simulation-task-details.jspx",
        "details", details);
  }
  
  @RequestMapping(value = "/vehicle-location-simulation!particle-details.do", method = RequestMethod.GET)
  public ModelAndView particleDetails(@RequestParam() int taskId, @RequestParam() int particleId) {

    VehicleLocationSimulationDetails details = _vehicleLocationSimulationService.getParticleDetails(taskId, particleId);
    return new ModelAndView("vehicle-location-simulation-task-details.jspx",
        "details", details);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-simulation-records.do", method = RequestMethod.GET)
  public void taskRecords(@RequestParam() int taskId,
      HttpServletResponse response) throws IOException {

    List<NycTestLocationRecord> records = _vehicleLocationSimulationService.getSimulationRecords(taskId);
    writeRecordsToOutput(response, records);
  }

  @RequestMapping(value = "/vehicle-location-simulation!task-result-records.do", method = RequestMethod.GET)
  public void taskParticles(@RequestParam() int taskId,
      HttpServletResponse response) throws IOException {

    List<NycTestLocationRecord> records = _vehicleLocationSimulationService.getResultRecords(taskId);
    writeRecordsToOutput(response, records);
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
      @RequestParam String properties) throws IOException {

    Date time = getTime(session, null);

    Properties props = new Properties();
    props.load(new StringReader(properties));

    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);

    _vehicleLocationSimulationService.addSimulationForBlockInstance(id,
        serviceDate, time.getTime(), bypassInference, props);

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
      List<NycTestLocationRecord> records) throws IOException {

    CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
    OutputStreamWriter writer = new OutputStreamWriter(
        response.getOutputStream());

    EntityHandler handler = factory.createWriter(NycTestLocationRecord.class,
        writer);
    
    if( records == null)
      records = Collections.emptyList();

    for (NycTestLocationRecord record : records)
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
