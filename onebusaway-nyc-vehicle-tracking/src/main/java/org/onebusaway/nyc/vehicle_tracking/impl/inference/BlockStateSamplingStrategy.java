package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;

public interface BlockStateSamplingStrategy {

  /****
   *
   ****/

  public CDFMap<BlockState> cdfForJourneyAtStart(Observation observation);

  public CDFMap<BlockState> cdfForJourneyInProgress(Observation observation);
}