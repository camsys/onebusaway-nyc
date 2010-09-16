package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public interface BlocksFromObservationService {

  /**
   * Compute the set of potential block instances that could apply to a
   * particular observation
   * 
   * @param observation
   * @return the probability map of potential block instances
   */
  public CDFMap<BlockState> determinePotentialBlocksForObservation(
      Observation observation);

  public BlockState advanceState(long timestamp, ProjectedPoint targetPoint,
      BlockState blockState, double maxDistanceToTravel);
}