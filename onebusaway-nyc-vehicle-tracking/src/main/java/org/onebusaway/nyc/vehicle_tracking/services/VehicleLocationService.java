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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleLocation;

/**
 * This is the main entry point to the location inference pipeline, where we we
 * accept raw SIRI {@link VehicleLocation} records and output processed location
 * data mapped to the best location+trip record.
 * 
 * @author bdferris
 * @see VehicleLocation
 */
public interface VehicleLocationService {

  public String getDefaultVehicleAgencyId();

  /**
   * Receive an incoming vehicle location SIRI record from a vehicle. This is an
   * unprocessed location record with destination sign code information.
   * 
   * @param siri the raw SIRI record
   * @param body
   */
  public void handleVehicleLocation(Siri siri, String body);

  /**
   * Convenience method for updating a vehicle location from data directly
   * 
   * @param time TODO
   * @param vehicleId
   * @param lat
   * @param lon
   * @param dsc destination sign code
   * @param saveResult should we internally save inference results for later
   *          access (useful for debugging)
   */
  public void handleVehicleLocation(long time, String vehicleId, double lat,
      double lon, String dsc, boolean saveResult);

  /**
   * Convenience method for bypassing inference completely and injecting a
   * completed vehicle location record into the data stream.
   */
  public void handleVehicleLocation(VehicleLocationRecord record);

  /**
   * 
   * @param record
   */
  public void handleNycTestLocationRecord(NycTestLocationRecord record);

  /**
   * Convenience method for clearing any active location inference data for the
   * specified vehicle
   * 
   * @param vehicleId
   */
  public void resetVehicleLocation(String vehicleId);

  /**
   * 
   * @param vehicleId
   * @return the most recent vehicle location record for the specified vehicle
   */
  public NycTestLocationRecord getVehicleLocationForVehicle(String vehicleId);

  /**
   * 
   * @param vehicleId
   * @return the most recent vehicle location management record for the
   *         specified vehicle
   */
  public VehicleLocationManagementRecord getVehicleLocationManagementRecordForVehicle(
      String vehicleId);

  /**
   * @return a list of the latest processed vehicle position records
   */
  public List<NycTestLocationRecord> getLatestProcessedVehicleLocationRecords();

  /**
   * @return a list of the most recent vehicle location management records
   */
  public List<VehicleLocationManagementRecord> getVehicleLocationManagementRecords();

  public void setVehicleStatus(String vehicleId, boolean enabled);

  /**
   * This is here primarily for debugging
   * 
   * @param vehicleId
   * @return the current list of particles for the specified vehicle
   */
  public List<Particle> getCurrentParticlesForVehicleId(String vehicleId);

  public List<Particle> getCurrentSampledParticlesForVehicleId(String vehicleId);

  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      String vehicleId);

  public VehicleLocationDetails getDetailsForVehicleId(String vehicleId);

  public VehicleLocationDetails getDetailsForVehicleId(String vehicleId,
      int particleId);

  public VehicleLocationDetails getBadDetailsForVehicleId(String vehicleId);

  public VehicleLocationDetails getBadDetailsForVehicleId(String vehicleId,
      int particleId);
}
