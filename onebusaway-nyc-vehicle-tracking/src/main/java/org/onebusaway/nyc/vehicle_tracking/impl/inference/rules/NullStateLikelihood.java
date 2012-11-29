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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NullStateLikelihood implements SensorModelRule {

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
  }

  private static final double nonNullLik = 0.9999d;
  
  static public enum NullStates {
    NULL_STATE, NON_NULL_STATE
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {
    final SensorModelResult result = new SensorModelResult("pNullState", 1.0);

    final NullStates state = getNullState(context);
    switch (state) {
      case NULL_STATE:
        result.addResultAsAnd("null-state", 1d - nonNullLik);
        break;
      case NON_NULL_STATE:
        result.addResultAsAnd("non-null-state", nonNullLik);
        break;
    }
    return result;
  }

  public static NullStates getNullState(Context context) {
    final VehicleState state = context.getState();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();
    EVehiclePhase phase = state.getJourneyState().getPhase();

    if (blockStateObs == null) {
      return NullStates.NULL_STATE;
    } else {

      final boolean hasScheduledTime = FastMath.abs(state.getBlockStateObservation().getScheduleDeviation()) > 0d;

      if (!hasScheduledTime) {
        return NullStates.NULL_STATE;
      }

      if (!state.getBlockStateObservation().isSnapped()
          && ((EVehiclePhase.DEADHEAD_AFTER == phase && state.getBlockStateObservation().getScheduleDeviation() == 0d)
              || EVehiclePhase.AT_BASE == phase
              || (EVehiclePhase.DEADHEAD_BEFORE == phase && state.getBlockStateObservation().getScheduleDeviation() == 0d) || (EVehiclePhase.LAYOVER_BEFORE == phase)
              && state.getBlockStateObservation().getScheduleDeviation() == 0d)) {
        return NullStates.NULL_STATE;
      }

      return NullStates.NON_NULL_STATE;
    }
  }

}
