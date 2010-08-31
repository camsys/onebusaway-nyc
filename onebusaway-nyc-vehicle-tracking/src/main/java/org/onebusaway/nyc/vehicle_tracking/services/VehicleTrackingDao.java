package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

/**
 * Methods for persisting and querying raw nyc vehicle location records from a
 * database.
 * 
 * @author bdferris
 * @see NycVehicleLocationRecord
 */
public interface VehicleTrackingDao {

  /**
   * Persist the specified destination sign code record to the database
   * 
   * @param record
   */
  public void saveOrUpdateDestinationSignCodeRecord(
      DestinationSignCodeRecord record);

  /**
   * 
   * @param destinationSignCode
   * @return a list of records with the matching destination sign code
   */
  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForDestinationSignCode(
      String destinationSignCode);

  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForTripId(
      AgencyAndId tripId);
}
