package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Set;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

public interface BlocksFromObservationService {

  public Set<BlockInstance> determinePotentialBlocksForObservation(
      Observation observation);

  public BlockState advanceState(long timestamp, ProjectedPoint targetPoint,
      BlockState blockState, double maxDistanceToTravel);
}