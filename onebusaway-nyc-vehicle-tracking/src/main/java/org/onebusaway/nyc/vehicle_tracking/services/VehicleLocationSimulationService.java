package org.onebusaway.nyc.vehicle_tracking.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public interface VehicleLocationSimulationService {

  public int simulateLocationsFromTrace(String traceType,
      InputStream traceInputStream, boolean runInRealtime,
      boolean pauseOnStart, boolean shiftStartTime, int minimumRecordInterval,
      boolean bypassInference, boolean fillActualProperties, boolean loop)
      throws IOException;

  public List<VehicleLocationSimulationSummary> getSimulations();

  public VehicleLocationSimulationSummary getSimulation(int taskId);

  public VehicleLocationSimulationDetails getSimulationDetails(int taskId,
      int historyOffset);

  public VehicleLocationSimulationDetails getParticleDetails(int taskId,
      int particleId);

  public List<NycTestLocationRecord> getSimulationRecords(int taskId);

  public List<NycTestLocationRecord> getResultRecords(int taskId);

  public void toggleSimulation(int taskId);

  public void stepSimulation(int taskId);

  public void stepSimulation(int taskId, int recordIndex);

  public void restartSimulation(int taskId);

  public void cancelSimulation(int taskid);

  public void cancelAllSimulations();

  public List<VehicleLocationSimulationSummary> getSimulationsForBlockInstance(
      AgencyAndId blockId, long serviceDate);

  public int addSimulationForBlockInstance(AgencyAndId blockId,
      long serviceDate, long actualTime, boolean bypassInference,
      boolean fillActualProperties, Properties properties);

}
