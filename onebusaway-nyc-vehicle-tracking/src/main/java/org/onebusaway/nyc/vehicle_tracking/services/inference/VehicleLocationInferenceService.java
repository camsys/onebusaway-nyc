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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;

import gov.sandia.cognition.statistics.DataDistribution;

import com.google.common.collect.Multiset;

import org.opentrackingtools.model.VehicleStateDistribution;

import java.util.List;
import java.util.concurrent.Future;

public interface VehicleLocationInferenceService {

  public void handleNycRawLocationRecord(NycRawLocationRecord record);

  public Future<?> handleNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record);

  public void handleBypassUpdateForNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record);

  public void handleRealtimeEnvelopeRecord(RealtimeEnvelope message);

  public void resetVehicleLocation(AgencyAndId vid);

  /**
   * Used by the simulator
   */
  public NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(
      AgencyAndId vid);

  public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords();

  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId);

  public void setSeeds(long cdfSeed, long factorySeed);

  public DataDistribution<VehicleStateDistribution<Observation>> getCurrentParticlesForVehicleId(AgencyAndId vehicleId);
}
