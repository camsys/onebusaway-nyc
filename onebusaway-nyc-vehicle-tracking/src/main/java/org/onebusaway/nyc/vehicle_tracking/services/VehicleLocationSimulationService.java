/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public interface VehicleLocationSimulationService {

  public List<VehicleLocationSimulationSummary> getSimulations();

  public VehicleLocationSimulationSummary getSimulation(int taskId);

  public VehicleLocationDetails getSimulationDetails(int taskId,
      int historyOffset);

  public List<NycTestInferredLocationRecord> getSimulationRecords(int taskId);

  public List<NycTestInferredLocationRecord> getResultRecords(int taskId);

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
      boolean isRunBased, boolean realtime, boolean fillActual,
      boolean reportsOperatorId, boolean reportsRunId,
      boolean allowRunTransitions, Properties properties);

  public VehicleLocationDetails getParticleDetails(int taskId, int particleId,
      int recordIndex);

  public int simulateLocationsFromTrace(String filename, String traceType,
      InputStream traceInputStream, boolean runInRealtime,
      boolean pauseOnStart, boolean shiftStartTime, int minimumRecordInterval,
      boolean bypassInference, boolean fillActualProperties, boolean loop,
      int historySize, String capiFile, InputStream capiInputStream, int capiStart) throws IOException;

  public VehicleLocationDetails getTransitionParticleDetails(int taskId,
      int particleId, int transParticleNum, int recordIndex);
}
