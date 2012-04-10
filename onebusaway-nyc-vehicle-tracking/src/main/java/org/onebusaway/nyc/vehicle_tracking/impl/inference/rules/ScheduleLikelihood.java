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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

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
import umontreal.iro.lecuyer.probdist.TruncatedDist;
import umontreal.iro.lecuyer.stat.Tally;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduleLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;

  /*
   * These are all in minutes
   */
  final static public StudentTDistribution schedDevPriorDist = new StudentTDistribution(2, 0d, 1d/15d);

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockState blockState = state.getBlockState();
    final VehicleState parentState = context.getParentState();
    
    final SensorModelResult result = computeSchedTimeProb(parentState, state, blockState, phase, obs);
    
    return result;
  }
  
  private SensorModelResult computeSchedTimeProb(VehicleState parentState, VehicleState state,
      BlockState blockState, EVehiclePhase phase, Observation obs) {
    final SensorModelResult result = new SensorModelResult("pSchedule", 1.0);
    if (blockState == null) {
      result.addLogResultAsAnd("null state", 0.0);
      
    } else if (EVehiclePhase.DEADHEAD_AFTER == phase) {
      
      if (FastMath.abs(state.getBlockStateObservation().getScheduleDeviation()) > 0d) {
        final double x = state.getBlockStateObservation().getScheduleDeviation();
        final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(x);
        result.addLogResultAsAnd("deadhead after", pSched);
      } else {
        result.addLogResultAsAnd("deadhead after", 0.0);
      }
      
    } else if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
      
      if (FastMath.abs(state.getBlockStateObservation().getScheduleDeviation()) > 0d) {
        final double x = state.getBlockStateObservation().getScheduleDeviation();
        final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(x);
        result.addLogResultAsAnd("deadhead before", pSched);
      } else {
        result.addLogResultAsAnd("deadhead before", 0.0);
      }
      
    } else if (EVehiclePhase.LAYOVER_BEFORE == phase) {
      
      if (FastMath.abs(state.getBlockStateObservation().getScheduleDeviation()) > 0d) {
        final double x = state.getBlockStateObservation().getScheduleDeviation();
        final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(x);
        result.addLogResultAsAnd("layover before", pSched);
      } else {
        result.addLogResultAsAnd("layover before", 0.0);
      }
      
    } else if (EVehiclePhase.DEADHEAD_DURING == phase) {
      
      final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(
          state.getBlockStateObservation().getScheduleDeviation());
      result.addLogResultAsAnd("deadhead during", pSched);
      
    } else if (EVehiclePhase.LAYOVER_DURING == phase) {
      
      final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(
          state.getBlockStateObservation().getScheduleDeviation());
      result.addLogResultAsAnd("layover during", pSched);
      
      
    } else if (EVehiclePhase.IN_PROGRESS == phase) {
      final double pSched = schedDevPriorDist.getProbabilityFunction().logEvaluate(
          state.getBlockStateObservation().getScheduleDeviation());
      result.addLogResultAsAnd("in progress", pSched);
    }

    return result;
  }
  
}
