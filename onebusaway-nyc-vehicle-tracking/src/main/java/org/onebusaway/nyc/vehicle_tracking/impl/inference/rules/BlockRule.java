package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.*;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.springframework.stereotype.Component;

@Component
public class BlockRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    SensorModelResult result = new SensorModelResult("pBlock");

    boolean activeDuringBlock = EVehiclePhase.isActiveDuringBlock(phase);
    
    /**
     * Rule: active during block => block assigned
     */
    double pBlockAssignedWhenActiveDuring = implies(p(activeDuringBlock),p(blockState != null));
    result.addResultAsAnd("active during block => block assigned", pBlockAssignedWhenActiveDuring);
    
    /**
     * Rule: vehicle in active phase => block assigned and not past the end of
     * the block
     */
    /*
    double p1 = implies(p(activeDuringBlock), p(blockState != null
        && blockState.getBlockLocation().getNextStop() != null));

    result.addResultAsAnd(
        "active phase => block assigned and not past the end of the block", p1);
    */

    /**
     * Rule: block assigned => on schedule
     */
    double pOnSchedule = 1.0;
    if (blockState != null
        && blockState.getBlockLocation().getNextStop() != null
        && activeDuringBlock)
      pOnSchedule = library.computeScheduleDeviationProbability(state, obs);
    
    result.addResultAsAnd("block assigned => on schedule", pOnSchedule);

    /**
     * Punish for traveling backwards
     */
    double pNoReverseTravel = 1.0;

    if (parentState != null && blockState != null) {
      BlockState parentBlockState = parentState.getBlockState();
      if (parentBlockState != null
          && parentBlockState.getBlockInstance().equals(
              blockState.getBlockInstance())) {
        ScheduledBlockLocation parentLocation = parentBlockState.getBlockLocation();
        ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
        if (blockLocation.getDistanceAlongBlock() + 20 < parentLocation.getDistanceAlongBlock())
          pNoReverseTravel = 0.05;
      }
    }
    
    result.addResultAsAnd("don't travel backwards", pNoReverseTravel);
    
    return result;
  }

}
