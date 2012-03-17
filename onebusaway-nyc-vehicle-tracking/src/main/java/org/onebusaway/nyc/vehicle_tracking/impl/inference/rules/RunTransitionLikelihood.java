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

import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import gov.sandia.cognition.evaluator.Evaluator;
import gov.sandia.cognition.learning.algorithm.tree.CategorizationTreeNode;
import gov.sandia.cognition.learning.algorithm.tree.DecisionTreeNode;
import gov.sandia.cognition.learning.function.categorization.AbstractBinaryCategorizer;
import gov.sandia.cognition.learning.function.categorization.BinaryCategorizer;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Set;

// @Component
public class RunTransitionLikelihood implements SensorModelRule {

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
  }
  
  public static enum RUN_TRANSITION_STATE {
    RUN_CHANGE_INFO_DIFF,
    RUN_CHANGE_FROM_OOS,
    RUN_CHANGE_FROM_IS,
    RUN_NOT_CHANGED
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    final SensorModelResult result = new SensorModelResult("pTransition", 1.0);
    
    RUN_TRANSITION_STATE state = getRunTransitionState(context);
    
    switch(state) {
      case RUN_CHANGE_INFO_DIFF:
        return result.addResultAsAnd("changed reported run or operator id", 0.9/2d);
      case RUN_CHANGE_FROM_OOS:
        return result.addResultAsAnd("change from o.o.s.", 0.1*(3d/4d));
      case RUN_CHANGE_FROM_IS:
        return result.addResultAsAnd("change not from o.o.s.", 0.1/4d);
      case RUN_NOT_CHANGED:
        return result.addResultAsAnd("not-changed run", 0.9/2d);
      default:
        return null;
    }
  }

  public RUN_TRANSITION_STATE getRunTransitionState(Context context) {
    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final Observation prevObs = obs.getPreviousObservation();


    if (parentState != null
        && MotionModelImpl.hasRunChanged(parentState.getBlockStateObservation(),
          state.getBlockStateObservation())) {
      if (!Objects.equal(obs.getRecord().getRunId(), prevObs.getRecord().getRunId())
           || !Objects.equal(obs.getRecord().getOperatorId(), prevObs.getRecord().getOperatorId())) {
        
        return RUN_TRANSITION_STATE.RUN_CHANGE_INFO_DIFF;
      } else {
        if (EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())
            || EVehiclePhase.DEADHEAD_AFTER == parentState.getJourneyState().getPhase()
            || EVehiclePhase.DEADHEAD_BEFORE == parentState.getJourneyState().getPhase()
            || EVehiclePhase.DEADHEAD_DURING == parentState.getJourneyState().getPhase()) {
          
          return RUN_TRANSITION_STATE.RUN_CHANGE_FROM_OOS;
        } else {
          return RUN_TRANSITION_STATE.RUN_CHANGE_FROM_IS;
        }
      }
    } else {
      return RUN_TRANSITION_STATE.RUN_NOT_CHANGED;
    }
  }
}
