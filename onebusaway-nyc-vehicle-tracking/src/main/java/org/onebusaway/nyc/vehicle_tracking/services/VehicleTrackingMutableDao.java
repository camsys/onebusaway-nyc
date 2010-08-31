package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

/**
 * Methods for persisting and querying raw nyc vehicle location records from a
 * database.
 * 
 * @author bdferris
 * @see NycVehicleLocationRecord
 */
public interface VehicleTrackingMutableDao {

  /**
   * Persist the specified record to the database
   * 
   * @param record
   */
  public void saveOrUpdateVehicleLocationRecord(NycVehicleLocationRecord record);

  /**
   * @param timeFrom - unix time (ms)
   * @param timeTo - unix time (ms)
   * @return records in the specified time range
   */
  public List<NycVehicleLocationRecord> getRecordsForTimeRange(long timeFrom,
      long timeTo);

}
