package org.onebusaway.nyc.vehicle_tracking.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface VehicleLocationSimulationService {

  public int simulateLocationsFromTrace(InputStream traceInputStream, boolean runInRealtime, boolean pauseOnStart) throws IOException;
  
  public List<VehicleLocationSimulationSummary> getSimulations();
  
  public VehicleLocationSimulationSummary getSimulation(int taskId);
  
  public void toggleSimulation(int taskId);
  
  public void stepSimulation(int taskId);
  
  public void cancelSimulation(int taskid);
}
