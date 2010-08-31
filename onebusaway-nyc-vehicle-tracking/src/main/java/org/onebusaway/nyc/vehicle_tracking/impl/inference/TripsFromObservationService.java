package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.transit_data_federation.services.tripplanner.TripInstanceProxy;

public interface TripsFromObservationService {

  /**
   * Compute the set of potential trip instances that could apply to a
   * particular observation
   * 
   * @param observation
   * @return the probability map of potential trip instances
   */
  public CDFMap<TripInstanceProxy> determinePotentialTripsForObservation(
      Observation observation);

}