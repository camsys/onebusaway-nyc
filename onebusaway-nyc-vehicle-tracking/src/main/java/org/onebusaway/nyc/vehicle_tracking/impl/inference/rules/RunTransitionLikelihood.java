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

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.stat.Tally;

import java.util.Set;
import java.util.concurrent.TimeUnit;

//@Component
public class RunTransitionLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;

  private DestinationSignCodeService _destinationSignCodeService;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }
  
  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final Observation prevObs = obs.getPreviousObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockState blockState = state.getBlockState();
    
    SensorModelResult result = new SensorModelResult("pTransition", 1.0);
    
    if (parentState != null && parentState.getBlockState() != null) {
      BlockState parentBlockState = parentState.getBlockState();
      if (!obs.getRecord().getRunId().equals(prevObs.getRecord().getRunId())) {
        result.addResultAsAnd("changed reported run", 1.0);
      } else if (!Objects.equal(obs.getRecord().getOperatorId(), prevObs.getRecord().getOperatorId())) {
        result.addResultAsAnd("changed operator id", 1.0);
//      } else if (_destinationSignCodeService.isUnknownDestinationSignCode(obs.getRecord().getDestinationSignCode())) {
//        result.addResultAsAnd("unknown dsc", 1.0);
      } else if (blockState == null){
        if (EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())
            || EVehiclePhase.isDeadhead(parentState.getJourneyState().getPhase())) {
//          result.addResultAsAnd("trans to null(prior)", 0.1);
        } else {
          result.addResultAsAnd("trans to null(active)", 0.01);
        }
      } else {
        result.addResultAsAnd("trans to not-null(prior)", 0.9);
        /*
         * Everything is fine regarding the observed values, now, how does the inferred
         * info fare?
         * First, consider if run is correct.
         */
        if (MotionModelImpl.hasRunChanged(parentState.getBlockStateObservation(), 
            state.getBlockStateObservation())) {
          result.addResultAsAnd("changed runs(prior)", 0.05);
          
          if (EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())
              || EVehiclePhase.isDeadhead(parentState.getJourneyState().getPhase())) {
//            result.addResultAsAnd("trans non-active(prior)", 0.8);
            /*
             * Transitioning right into deadhead-after?
             */
            if (EVehiclePhase.DEADHEAD_AFTER == phase) {
              result.addResultAsAnd("trans to deadhead-after", 0.001);
            } else {
//              result.addResultAsAnd("trans to not-deadhead-after", 0.99);
            }
          } else {
            result.addResultAsAnd("trans active(prior)", 0.001);
          }
          
//          final boolean wasAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
//              prevObs.getRecord(),
//              parentBlockState.getBlockInstance());
//          final boolean isAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
//              obs.getRecord(),
//              parentBlockState.getBlockInstance());
//          boolean throughTerminal = wasAtBlockTerminal && !isAtBlockTerminal;
//          
//          if (throughTerminal) {
//  //          result.addResultAsAnd("through-terminal", 0.9);
//          } else {
//            result.addResultAsAnd("not through-terminal", 0.25);
//          }
        } else {
          result.addResultAsAnd("didn't change runs(prior)", 0.95);
        }
        
      }
    } else {
      result.addResultAsAnd("no parent", 1.0);
    }
    
    return result;
  }
}

