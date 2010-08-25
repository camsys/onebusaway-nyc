package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
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

  @Autowired
  public void setVehicleLocationSimulationService(
      VehicleLocationSimulationService service) {
    _vehicleLocationSimulationService = service;
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

  @RequestMapping(value = "/vehicle-location-simulation!task-data.do", method = RequestMethod.GET)
  public ModelAndView taskData(@RequestParam() int taskId) {

    VehicleLocationSimulationSummary summary = _vehicleLocationSimulationService.getSimulation(taskId);
    return new ModelAndView("json", "summary", summary);
  }
}
