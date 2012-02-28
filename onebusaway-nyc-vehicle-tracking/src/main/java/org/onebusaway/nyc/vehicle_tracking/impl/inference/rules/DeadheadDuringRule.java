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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.not;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeadheadDuringRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_DURING || blockState == null)
      return new SensorModelResult("pDeadheadDuring (n/a)");

    final SensorModelResult result = new SensorModelResult("pDeadheadDuring");

    /**
     * Rule: DEADHEAD_DURING <=> Vehicle has moved AND at layover location
     */

    final double pMoved = not(SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
        state.getMotionState(), obs));

    result.addResultAsAnd(
        "DEADHEAD_DURING <=> Vehicle has moved", pMoved);

    /**
     * Rule: DEADHEAD_DURING => not right on block
     */
    final double pDistanceFromBlock = library.computeDeadheadDistanceFromBlockProbability(
        obs, blockState);
    result.addResultAsAnd("DEADHEAD_DURING => not right on block",
        pDistanceFromBlock);

    /**
     * Rule: DEADHEAD_DURING => resume block on time
     */
    final double pStartBlockOnTime = library.computeStartOrResumeBlockOnTimeProbability(
        state, obs);
    result.addResultAsAnd("DEADHEAD_DURING => resume block on time",
        pStartBlockOnTime);

//    /**
//     * Rule: DEADHEAD_DURING => served some part of block
//     */
//    final double pServedSomePartOfBlock = library.computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
//    result.addResultAsAnd("DEADHEAD_DURING => served some part of block",
//        pServedSomePartOfBlock);

    return result;
  }

}
