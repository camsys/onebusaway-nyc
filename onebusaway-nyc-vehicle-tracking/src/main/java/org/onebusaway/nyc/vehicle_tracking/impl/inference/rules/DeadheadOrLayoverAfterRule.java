package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.or;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.stereotype.Component;

@Component
public class DeadheadOrLayoverAfterRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    /**
     * Why do we check for a block state here? We assume a vehicle can't
     * deadhead or layover after unless it's actively served a block
     */
    if (!EVehiclePhase.isActiveAfterBlock(phase) || blockState == null)
      return new SensorModelResult("pAfter (n/a)");

    SensorModelResult result = new SensorModelResult("pAfter");

    /**
     * RULE: DEADHEAD_AFTER || LAYOVER_AFTER => reached the end of the block
     */

    double pEndOfBlock = library.computeProbabilityOfEndOfBlock(state.getBlockState());
    result.addResult("pEndOfBlock", pEndOfBlock);

    /**
     * There is always some chance that the vehicle has served some part of the
     * block and then gone rogue: out-of-service or off-block. Unfortunately,
     * this is tricky because a vehicle could have been incorrectly assigned to
     * a block ever so briefly (fulfilling the served-some-part-of-the-block
     * requirement), it can quickly go off-block if the block assignment doesn't
     * keep up.
     */
    double pServedSomePartOfBlock = library.computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
    double pOffBlock = library.computeOffBlockProbability(state, obs);
    double pOutOfService = library.computeOutOfServiceProbability(obs);

    double pOffRouteOrOutOfService = pServedSomePartOfBlock
        * or(pOffBlock, pOutOfService);

    SensorModelResult r = result.addResult("pOffRouteOrOutOfService",
        pOffRouteOrOutOfService);
    r.addResult("pServedSomePartOfBlock", pServedSomePartOfBlock);
    r.addResult("pOffBlock", pOffBlock);
    r.addResult("pOutOfService", pOutOfService);

    double p = or(pEndOfBlock, pOffRouteOrOutOfService);
    result.setProbability(p);

    return result;
  }

}
