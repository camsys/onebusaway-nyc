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

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
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
public class TransitionRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    Observation prevObs = obs.getPreviousObservation();

    if (parentState == null || prevObs == null)
      return new SensorModelResult("pTransition (n/a)");

    JourneyState parentJourneyState = parentState.getJourneyState();
    JourneyState journeyState = state.getJourneyState();

    EVehiclePhase parentPhase = parentJourneyState.getPhase();
    EVehiclePhase phase = journeyState.getPhase();

    SensorModelResult result = new SensorModelResult("pTransition");

    /**
     * Transition Before to During => left terminal || transitioned to in
     * service
     */
    boolean transitionBeforeToDuring = EVehiclePhase.isActiveBeforeBlock(parentPhase)
        && EVehiclePhase.isActiveDuringBlock(phase);
    boolean justLeftTerminal = false;
    if (parentState.getBlockState() != null) {
      boolean wasAtBlockTerminal = _vehicleStateLibrary.isAtPotentialTerminal(
          prevObs.getRecord(), parentState.getBlockState().getBlockInstance());
      boolean isAtBlockTerminal = _vehicleStateLibrary.isAtPotentialTerminal(
          obs.getRecord(), parentState.getBlockState().getBlockInstance());

      justLeftTerminal = wasAtBlockTerminal && !isAtBlockTerminal;
    } else {
      justLeftTerminal = prevObs.isAtTerminal() && !obs.isAtTerminal();
    }

    boolean pOutToInService = prevObs.isOutOfService() && !obs.isOutOfService();

    // double pTransitionFromBeforeToDuring =
    // implies(p(transitionBeforeToDuring),
    // p(justLeftTerminal || pOutToInService));
    //
    // result.addResultAsAnd("Transition Before to During => left terminal",
    // pTransitionFromBeforeToDuring);

    /**
     * Transition During to Before => out of service or at base
     */
    boolean transitionDuringToBefore = EVehiclePhase.isActiveDuringBlock(parentPhase)
        && EVehiclePhase.isActiveBeforeBlock(phase);

    boolean wasOffRoute = false;
    if (parentState.getBlockState() != null)
      wasOffRoute = _vehicleStateLibrary.isOffBlock(prevObs,
          parentState.getBlockState());

    boolean endOfBlock = false;
    BlockState blockState = state.getBlockState();
    if (blockState != null
        && (blockState.getBlockLocation().getNextStop() == null || library.computeProbabilityOfEndOfBlock(blockState) > 0.9))
      endOfBlock = true;

    /**
     * Added this hack to allow no block transitions after finishing one.
     */
    boolean wasAtEndOfBlock = false;
    BlockState parentBlockState = parentState.getBlockState() != null
        ? parentState.getBlockState() : null;
    if (parentBlockState != null
        && blockState == null
        && (parentBlockState.getBlockLocation().getNextStop() == null || library.computeProbabilityOfEndOfBlock(parentBlockState) > 0.9))
      wasAtEndOfBlock = true;

    double pTransitionFromDuringToBefore = implies(p(transitionDuringToBefore),
        p(obs.isAtBase() || obs.isOutOfService() || wasOffRoute || endOfBlock
            || wasAtEndOfBlock));

    result.addResultAsAnd(
        "Transition During to Before => out of service OR at base OR was off route OR just finished block",
        pTransitionFromDuringToBefore);

    /**
     * just left terminal AND in-service => active during. the following
     * condition essentially means we allow a bus to deadhead toward base after
     * going through the last terminal with a valid dsc.
     * 
     */
    // if (!EVehiclePhase.isActiveAfterBlock(phase) && blockState != null) {
    // double pInService = implies(p(justLeftTerminal && !obs.isOutOfService()),
    // p(EVehiclePhase.isActiveDuringBlock(phase)));
    // result.addResultAsAnd(
    // "just left terminal AND in-service => active during", pInService);
    // }

    return result;
  }
}
