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

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import org.springframework.beans.factory.annotation.Autowired;

// @Component
public class TransitionRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {

    final VehicleState parentState = context.getParentState();
    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final Observation prevObs = obs.getPreviousObservation();

    if (parentState == null || prevObs == null)
      return new SensorModelResult("pTransition (n/a)");

    final JourneyState parentJourneyState = parentState.getJourneyState();
    final JourneyState journeyState = state.getJourneyState();

    final EVehiclePhase parentPhase = parentJourneyState.getPhase();
    final EVehiclePhase phase = journeyState.getPhase();

    final SensorModelResult result = new SensorModelResult("pTransition");

    /**
     * Transition During to Before => out of service or at base
     */
    final boolean transitionDuringToBefore = EVehiclePhase.isActiveDuringBlock(parentPhase)
        && EVehiclePhase.isActiveBeforeBlock(phase);

    boolean wasOffRoute = false;
    if (parentState.getBlockState() != null)
      wasOffRoute = _vehicleStateLibrary.isOffBlock(prevObs,
          parentState.getBlockState());

    boolean endOfBlock = false;
    final BlockState blockState = state.getBlockState();
    if (blockState != null
        && (blockState.getBlockLocation().getNextStop() == null || SensorModelSupportLibrary.computeProbabilityOfEndOfBlock(blockState) > 0.9))
      endOfBlock = true;

    /**
     * Added this hack to allow no block transitions after finishing one.
     */
    boolean wasAtEndOfBlock = false;
    final BlockState parentBlockState = parentState.getBlockState() != null
        ? parentState.getBlockState() : null;
    if (parentBlockState != null
        && blockState == null
        && (parentBlockState.getBlockLocation().getNextStop() == null || SensorModelSupportLibrary.computeProbabilityOfEndOfBlock(parentBlockState) > 0.9))
      wasAtEndOfBlock = true;

    final double pTransitionFromDuringToBefore = implies(
        p(transitionDuringToBefore),
        p(obs.isAtBase() || obs.hasOutOfServiceDsc() || wasOffRoute
            || endOfBlock || wasAtEndOfBlock));

    result.addResultAsAnd(
        "Transition During to Before => out of service OR at base OR was off route OR just finished block",
        pTransitionFromDuringToBefore);

    return result;
  }
}
