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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.stereotype.Component;


@Component
public class NullLocationLikelihood implements SensorModelRule {

  static public enum NullLocationStates {
    NULL_STATE,
    NON_NULL_STATE
  }
  
  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    SensorModelResult result = new SensorModelResult("pNullLocationState", 1.0);
  
    NullLocationStates state = getNullLocationState(context);
    switch(state) {
      case NULL_STATE:
        result.addResultAsAnd("null-state", 0.01);
        break;
      case NON_NULL_STATE:
        result.addResultAsAnd("non-null-state", 0.99);
        break;
    }
    return result;
  }
    
  public static NullLocationStates getNullLocationState(Context context) {
    final VehicleState state = context.getState();
    final BlockState blockState = state.getBlockState();
    EVehiclePhase phase = state.getJourneyState().getPhase();
    
    /*
     * TODO clean up this hack
     * We are really in-progress, but because of the out-of-service
     * headsign, we can't report it as in-progress
     */
    if (context.getObservation().isOutOfService()
        && EVehiclePhase.DEADHEAD_DURING == phase
        && (blockState != null && JourneyStateTransitionModel.isLocationOnATrip(blockState)))
      phase = EVehiclePhase.IN_PROGRESS;
    
    if (blockState == null) {
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

