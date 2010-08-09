package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

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
   * @param vehicleId
   * @param lat
   * @param lon
   * @param dsc destination sign code
   */
  public void handleVehicleLocation(String vehicleId, double lat, double lon,
      String dsc);

  /**
   * @return a list of the latest processed vehicle position records
   */
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords();

}
