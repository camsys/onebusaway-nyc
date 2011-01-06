package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Set;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

public interface BlocksFromObservationService {

  public Set<BlockInstance> determinePotentialBlocksForObservation(
      Observation observation);

  public BlockState advanceState(Observation observation, BlockState blockState,
      double minDistanceToTravel, double maxDistanceToTravel);

  public BlockState advanceLayoverState(long timestamp, BlockState blockState);

  public BlockState bestState(Observation observation, BlockState blockState);
}