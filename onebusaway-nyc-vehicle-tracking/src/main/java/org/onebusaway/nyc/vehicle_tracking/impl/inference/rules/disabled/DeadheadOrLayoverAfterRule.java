/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.disabled;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.or;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

//@Component
public class DeadheadOrLayoverAfterRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    /**
     * Why do we check for a block state here? We assume a vehicle can't
     * deadhead or layover after unless it's actively served a block
     */
    if (!EVehiclePhase.isActiveAfterBlock(phase) || blockState == null)
      return new SensorModelResult("pAfter (n/a)");

    final SensorModelResult result = new SensorModelResult("pAfter");

    /**
     * RULE: DEADHEAD_AFTER || LAYOVER_AFTER => reached the end of the block
     */

    final double pEndOfBlock = SensorModelSupportLibrary.computeProbabilityOfEndOfBlock(blockState);
    result.addResult("pEndOfBlock", pEndOfBlock);

    /**
     * There is always some chance that the vehicle has served some part of the
     * block and then gone rogue: out-of-service or off-block. Unfortunately,
     * this is tricky because a vehicle could have been incorrectly assigned to
     * a block ever so briefly (fulfilling the served-some-part-of-the-block
     * requirement), it can quickly go off-block if the block assignment doesn't
     * keep up.
     */
    final double pServedSomePartOfBlock = library.computeProbabilityOfServingSomePartOfBlock(blockState);
    final double pOffBlock = library.computeOffBlockProbability(state, obs);
    // double pOutOfService = library.computeOutOfServiceProbability(obs);
    final double pOutOfService = obs.isOutOfService() ? 1.0 : 0.0;

    final double pOffRouteOrOutOfService = pServedSomePartOfBlock
        * or(pOffBlock, pOutOfService);

    final SensorModelResult r = result.addResult("pOffRouteOrOutOfService",
        pOffRouteOrOutOfService);
    r.addResult("pServedSomePartOfBlock", pServedSomePartOfBlock);
    r.addResult("pOffBlock", pOffBlock);
    r.addResult("pOutOfService", pOutOfService);

    final double p = or(pEndOfBlock, pOffRouteOrOutOfService);
    result.setProbability(p);

    return result;
  }

}
