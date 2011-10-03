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
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

/**
 * NOTE: This rule has been disabled
 * 
 * @author bdferris
 * 
 */
// TODO re-institute this for run transitions?
// @Component
public class BlockSwitchRule implements SensorModelRule {

  /**
   * What are the odds of switching blocks mid-stream? LOW!
   */
  private double _probabilityOfBlockChangeWhileInProgress = 0.01;

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    if (parentState == null)
      return new SensorModelResult("pBlockSwitch (n/a: no parent)");

    BlockState parentBlockState = parentState.getBlockState();
    BlockState blockState = state.getBlockState();

    if (parentBlockState == null)
      return new SensorModelResult("pBlockSwitch (n/a: no parent block)");

    if (blockState == null)
      return new SensorModelResult("pBlockSwitch (n/a: no block)");

    if (parentBlockState.getBlockInstance().equals(
        blockState.getBlockInstance()))
      return new SensorModelResult("pBlockSwitch (n/a: no block switch)");

    Observation prevObs = obs.getPreviousObservation();

    double pLeavingTerminal = p(prevObs.isAtTerminal() && !obs.isAtTerminal());
    double pSignCodeChanged = p(
        BlockStateTransitionModel.hasDestinationSignCodeChangedBetweenObservations(obs),
        0.95);
    double pRandomBlockChange = _probabilityOfBlockChangeWhileInProgress;
    double pBlockChangeAtThisLocation = or(pLeavingTerminal, pSignCodeChanged,
        pRandomBlockChange);

    SensorModelResult result = new SensorModelResult("pBlockSwitch",
        pBlockChangeAtThisLocation);
    result.addResult("pLeavingTerminal", pLeavingTerminal);
    result.addResult("pSignCodeChanged", pSignCodeChanged);
    result.addResult("pRandomBlockChange", pRandomBlockChange);
    return result;
  }

}
