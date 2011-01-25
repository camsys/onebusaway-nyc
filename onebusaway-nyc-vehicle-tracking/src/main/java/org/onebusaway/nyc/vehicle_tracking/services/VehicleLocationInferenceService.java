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
package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;

public interface VehicleLocationInferenceService {

  public void handleNycVehicleLocationRecord(NycVehicleLocationRecord record);

  public void handleVehicleLocationRecord(VehicleLocationRecord record);

  public void handleNycTestLocationRecord(AgencyAndId vehicleId,
      NycTestLocationRecord record);

  public void resetVehicleLocation(AgencyAndId vid);

  public NycTestLocationRecord getVehicleLocationForVehicle(AgencyAndId vid);

  public List<NycTestLocationRecord> getLatestProcessedVehicleLocationRecords();

  public VehicleLocationManagementRecord getVehicleLocationManagementRecordForVehicle(
      AgencyAndId vid);

  public List<VehicleLocationManagementRecord> getVehicleLocationManagementRecords();

  public void setVehicleStatus(AgencyAndId vid, boolean enabled);

  /**
   * This is primarily here for debugging
   * 
   * @param vehicleId
   * @return the most recent list of particles for the specified vehicle
   */
  public List<Particle> getCurrentParticlesForVehicleId(AgencyAndId vehicleId);

  public List<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId);

  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId);

  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId agencyAndId);

  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId);
}
