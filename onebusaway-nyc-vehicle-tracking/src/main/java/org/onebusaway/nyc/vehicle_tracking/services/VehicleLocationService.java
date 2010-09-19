package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
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

  /**
   * Receive an incoming vehicle location SIRI record from a vehicle. This is an
   * unprocessed location record with destination sign code information.
   * 
   * @param siri the raw SIRI record
   */
  public void handleVehicleLocation(Siri siri);

  /**
   * Convenience method for updating a vehicle location from data directly
   * 
   * @param time TODO
   * @param vehicleId
   * @param lat
   * @param lon
   * @param dsc destination sign code
   * @param saveResult should we internally save inference results for later access (useful for debugging)
   */
  public void handleVehicleLocation(long time, String vehicleId, double lat,
      double lon, String dsc, boolean saveResult);

  /**
   * Convenience method for clearing any active location inference data for the
   * specified vehicle
   * 
   * @param vehicleId
   */
  public void resetVehicleLocation(String vehicleId);

  public VehicleLocationRecord getVehicleLocationForVehicle(String vehicleId);

  /**
   * @return a list of the latest processed vehicle position records
   */
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords();

  /**
   * This is here primarily for debugging
   * 
   * @param vehicleId
   * @return the current list of particles for the specified vehicle
   */
  public List<Particle> getCurrentParticlesForVehicleId(String vehicleId);

  /**
   * This is here primarily for debugging
   * 
   * @param vehicleId
   * @return the most likely list of particles for the specified vehicle
   */
  public List<Particle> getMostLikelyParticlesForVehicleId(String vehicleId);
}
