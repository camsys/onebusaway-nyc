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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;

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

@Component
public class NullStateLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  static public enum NullStates {
    NULL_STATE,
    NON_NULL_STATE
  }
  
  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    SensorModelResult result = new SensorModelResult("pNullState", 1.0);
  
    NullStates state = getNullState(context);
    switch(state) {
      case NULL_STATE:
        result.addResultAsAnd("null-state", 0.005);
        break;
      case NON_NULL_STATE:
        result.addResultAsAnd("non-null-state", 0.995);
        break;
    }
    return result;
  }
    
  public static NullStates getNullState(Context context) {
    final VehicleState state = context.getState();
//    final Observation obs = context.getObservation();
    final BlockState blockState = state.getBlockState();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    
    if (blockState == null) {
      return NullStates.NULL_STATE;
    } else {
      
      final boolean hasScheduledTime = 
          FastMath.abs(state.getBlockStateObservation().getScheduleDeviation()) > 0d;
          
      if (!hasScheduledTime) {
        return NullStates.NULL_STATE;
      }
      
      if (!state.getBlockStateObservation().isSnapped()
          && (EVehiclePhase.DEADHEAD_AFTER == phase
            || EVehiclePhase.AT_BASE == phase
            || EVehiclePhase.DEADHEAD_BEFORE == phase
            || EVehiclePhase.LAYOVER_BEFORE == phase)) {
        return NullStates.NULL_STATE;
      } 
      
      return NullStates.NON_NULL_STATE;
    }
  }
  
}

