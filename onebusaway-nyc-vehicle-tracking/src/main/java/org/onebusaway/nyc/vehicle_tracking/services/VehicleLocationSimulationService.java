package org.onebusaway.nyc.vehicle_tracking.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public interface VehicleLocationSimulationService {

  public int simulateLocationsFromTrace(InputStream traceInputStream, boolean runInRealtime, boolean pauseOnStart, boolean shiftStartTime) throws IOException;
  
  public List<VehicleLocationSimulationSummary> getSimulations();
  
  public VehicleLocationSimulationSummary getSimulation(int taskId);
  
  public VehicleLocationSimulationDetails getSimulationDetails(int taskId);
  
  public List<NycTestLocationRecord> getSimulationRecords(int taskId);
  
  public void toggleSimulation(int taskId);
  
  public void stepSimulation(int taskId);
  
  public void cancelSimulation(int taskid);
  
  public List<VehicleLocationSimulationSummary> getSimulationsForBlockInstance(AgencyAndId blockId, long serviceDate);
  
  public int addSimulationForBlockInstance(AgencyAndId blockId, long serviceDate, Properties properties);
}
