package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForAgencyQueryBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.beans.BlockBeanService;
import org.onebusaway.transit_data_federation.services.beans.BlockStatusBeanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VehicleLocationSimulationController {

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

  @RequestMapping(value = "/vehicle-location-simulation.do", method = RequestMethod.GET)
  public ModelAndView index() {
    List<VehicleLocationSimulationSummary> simulations = _vehicleLocationSimulationService.getSimulations();
    return new ModelAndView("vehicle-location-simulation.jspx", "simulations",
        simulations);
  }

  @RequestMapping(value = "/vehicle-location-simulation!upload-trace.do", method = RequestMethod.POST)
  public ModelAndView uploadTrace(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "realtime", required = false, defaultValue = "false") boolean realtime,
      @RequestParam(value = "pauseOnStart", required = false, defaultValue = "false") boolean pauseOnStart)
      throws IOException {

    if (!file.isEmpty()) {

      String name = file.getOriginalFilename();
      InputStream in = file.getInputStream();

      if (name.endsWith(".gz"))
        in = new GZIPInputStream(in);

      _vehicleLocationSimulationService.simulateLocationsFromTrace(in,
          realtime, pauseOnStart);
    }

    return new ModelAndView("redirect:/vehicle-location-simulation.do");
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
    return new ModelAndView("vehicle-location-simulation-task-details.jspx", "details", details);
  }

  @RequestMapping(value = "/vehicle-location-simulation!active-blocks.do", method = RequestMethod.GET)
  public ModelAndView activeBlocks() {

    List<BlockDetailsBean> blocks = new ArrayList<BlockDetailsBean>();

    for (String agencyId : _agencyService.getAllAgencyIds()) {
      TripsForAgencyQueryBean query = new TripsForAgencyQueryBean();
      query.setAgencyId(agencyId);
      query.setInclusion(new TripDetailsInclusionBean(false, false, true));
      query.setMaxCount(Integer.MAX_VALUE);
      query.setTime(System.currentTimeMillis());
      
      ListBean<BlockDetailsBean> beans = _blockStatusBeanService.getBlocksForAgency(query);
      blocks.addAll(beans.getList());
    }

    return new ModelAndView("vehicle-location-simulation-active-blocks.jspx", "blocks", blocks);
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
  public ModelAndView addBlockSimulation(@RequestParam String blockId,
      @RequestParam long serviceDate, @RequestParam String parameters) {

    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    _vehicleLocationSimulationService.addSimulationForBlockInstance(id,
        serviceDate, parameters);
    
    Map<String, Object> model = new HashMap<String, Object>();
    model.put("blockId", blockId);
    model.put("serviceDate", serviceDate);
    return new ModelAndView("redirect:/vehicle-location-simulation!block.do",
        model);
  }
}
