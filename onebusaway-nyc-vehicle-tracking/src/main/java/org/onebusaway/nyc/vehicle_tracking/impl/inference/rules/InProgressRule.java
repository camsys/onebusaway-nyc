package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.stereotype.Component;

@Component
public class InProgressRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    /**
     * These probabilities only apply if are IN_PROGRESS and have a block state
     */
    if (phase != EVehiclePhase.IN_PROGRESS || blockState == null)
      return new SensorModelResult("pInProgress (n/a)");

    SensorModelResult result = new SensorModelResult("pInProgress");

    /**
     * Rule: IN_PROGRESS => block location is close to gps location
     */
    double pBlockLocation = library.computeBlockLocationProbability(
        parentState, blockState, obs);
    result.addResultAsAnd("pBlockLocation", pBlockLocation);

    return result;
  }

}
