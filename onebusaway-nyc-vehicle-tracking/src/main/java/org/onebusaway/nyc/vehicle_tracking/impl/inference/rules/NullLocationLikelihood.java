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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import org.springframework.stereotype.Component;

@Component
public class NullLocationLikelihood implements SensorModelRule {

  static public enum NullLocationStates {
    NULL_STATE, NON_NULL_STATE
  }

  static private double nullLocProb = 5e-3;

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {
    final SensorModelResult result = new SensorModelResult(
        "pNullLocationState", 1.0);

    final NullLocationStates state = getNullLocationState(context);
    switch (state) {
      case NULL_STATE:
        result.addResultAsAnd("null-state", nullLocProb);
        break;
      case NON_NULL_STATE:
        result.addResultAsAnd("non-null-state", 1d - nullLocProb);
        break;
    }
    return result;
  }

  public static NullLocationStates getNullLocationState(Context context) {
    final VehicleState state = context.getState();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();
    EVehiclePhase phase = state.getJourneyState().getPhase();

    if (blockStateObs == null) {
      return NullLocationStates.NULL_STATE;
    } else {

      if (state.getBlockStateObservation().isSnapped()) {
        return NullLocationStates.NON_NULL_STATE;
      }

      if (EVehiclePhase.DEADHEAD_AFTER == phase
          || EVehiclePhase.AT_BASE == phase
          || EVehiclePhase.DEADHEAD_BEFORE == phase
          || EVehiclePhase.DEADHEAD_DURING == phase
          || EVehiclePhase.LAYOVER_DURING == phase
          || EVehiclePhase.LAYOVER_BEFORE == phase) {
        return NullLocationStates.NULL_STATE;
      }

      return NullLocationStates.NON_NULL_STATE;
    }
  }

}
