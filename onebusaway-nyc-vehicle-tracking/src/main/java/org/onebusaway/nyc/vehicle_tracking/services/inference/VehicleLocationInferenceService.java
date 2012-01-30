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
package org.onebusaway.nyc.vehicle_tracking.services.inference;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;

public interface VehicleLocationInferenceService {

  public void handleNycRawLocationRecord(NycRawLocationRecord record);

  public void handleNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record);

  public void handleRealtimeEnvelopeRecord(RealtimeEnvelope message);

  public void resetVehicleLocation(AgencyAndId vid);

  public void setVehicleStatus(AgencyAndId vid, boolean enabled);

  /**
   * Used by the simulator
   */
  public NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(
      AgencyAndId vid);

  public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords();

  /**
   * These are primarily here for debugging
   */
  public List<Particle> getCurrentParticlesForVehicleId(AgencyAndId vehicleId);

  public List<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId);

  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId agencyAndId);

  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId);

  public VehicleLocationDetails getBadDetailsForVehicleId(
      AgencyAndId vehicleId, int particleId);

  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId);

  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId,
      int particleId);

  public void setSeeds(long cdfSeed, long factorySeed);
}
