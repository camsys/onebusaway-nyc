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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
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
public class EdgeLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;

  final private double distStdDev = 50.0;
  final private double gpsStdDev = 50.0;
  final private double gpsMean = 10.0;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockState blockState = state.getBlockState();
    
    SensorModelResult result = new SensorModelResult("pEdge", 1.0);
    
    if (obs.getPreviousObservation() == null || parentState == null) {
      if (EVehiclePhase.isActiveDuringBlock(phase)) {
        result.addResultAsAnd("no prev. obs./vehicle-state", 0.0);
      } else if (blockState == null 
          || !state.getBlockStateObservation().isSnapped()){
        result.addResultAsAnd("no prev. obs./vehicle-state nor blockState or snapped loc.", 1.0);
      }
      return result;
    }
    
    if (blockState == null) {
      if (obs.isAtBase())
        result.addResultAsAnd("pNotInProgress(base)", 1.0);
      else 
        result.addResultAsAnd("pNotInProgress(prior)", 0.5);
      return result;
    }

    /*
     * GPS Error
     */
    final double pGps;
    if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
      pGps = 0.5;
    } else if (EVehiclePhase.DEADHEAD_DURING == phase) {
      final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
      final ProjectedPoint p2 = obs.getPoint();
  
      final double d = SphericalGeometryLibrary.distance(p1.getLat(),
          p1.getLon(), p2.getLat(), p2.getLon());
      pGps = FoldedNormalDist.cdf(gpsMean, gpsStdDev, d);
    } else {
      final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
      final ProjectedPoint p2 = obs.getPoint();
  
      final double d = SphericalGeometryLibrary.distance(p1.getLat(),
          p1.getLon(), p2.getLat(), p2.getLon());
      pGps = 1.0 - FoldedNormalDist.cdf(gpsMean, gpsStdDev, d);
    }
    result.addResult("gps", pGps);
    
    /*
     * Edge Movement
     */
    Set<BlockState> prevBlockStates = Sets.newHashSet();
    
    boolean previouslyInactive = 
        parentState.getBlockState() == null 
        || !parentState.getBlockState().getBlockInstance().equals(blockState.getBlockInstance());
    if (!previouslyInactive) {
      prevBlockStates.add(parentState.getBlockState());
    } else {
      try {
        final BestBlockStates prevBestStates = _blockStateService.getBestBlockLocations(
            obs.getPreviousObservation(), blockState.getBlockInstance(), 0,
            Double.POSITIVE_INFINITY);

        if (prevBestStates != null)
          prevBlockStates.addAll(prevBestStates.getAllStates());

      } catch (final MissingShapePointsException e) {
        e.printStackTrace();
        return result.addResultAsAnd("missing shapepoints", 0.0);
      }
    }
    
    final double pDistAlong;
    if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
      pDistAlong = 1.0 - FoldedNormalDist.cdf(0.0, gpsStdDev, 
          blockState.getBlockLocation().getDistanceAlongBlock());
      
    } else if (EVehiclePhase.DEADHEAD_DURING == phase) {
      if (!prevBlockStates.isEmpty()){
        pDistAlong = 1.0 - computeEdgeMovementProb(blockState, obs, prevBlockStates);
        result.addResult("distance", pDistAlong);
      } else {
        pDistAlong = 0.5;
        result.addResult("pNoPrevState", 0.5);
      }
    } else {
      if (!prevBlockStates.isEmpty()){
        pDistAlong = computeEdgeMovementProb(blockState, obs, prevBlockStates);
        result.addResult("distance", pDistAlong);
      } else {
        pDistAlong = 0.5;
        result.addResult("pNoPrevState", 0.5);
      }
    }
    
    final double totalProb = FastMath.exp(FastMath.log(pGps) + FastMath.log(pDistAlong));
    result.setProbability(totalProb);
    return result;
  }
  
  private final double computeEdgeMovementProb(BlockState blockState, Observation obs,
      Set<BlockState> prevBlockStates) {
    final double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();

    final Tally avgRes = new Tally();

    final double obsDelta = SphericalGeometryLibrary.distance(obs.getLocation(), 
        obs.getPreviousObservation().getLocation());
    
    /*
     * If the movement is too small, then there's a fair chance 
     * that it's not really moving at all, so we can't really tell
     * if the observed movement is indicative of this trip or not.
    */
    if (obsDelta < 10)
      return 0.5;
    
    /*
     * When there are multiple (potential) previously in-progress block-states,
     * we need to average over them to determine
     */
    for (final BlockState prevBlockState : prevBlockStates) {
      final double prevDab = prevBlockState.getBlockLocation().getDistanceAlongBlock();
      double dabDelta = currentDab - prevDab;
      
//      /*
//       * Use whichever comes first: previous time incremented by observed change
//       * in time, or last stop departure time.
//       */
//      final int expSchedTime = Math.min(
//          (int) (prevBlockState.getBlockLocation().getScheduledTime() 
//              + (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000),
//          Iterables.getLast(
//              prevBlockState.getBlockInstance().getBlock().getStopTimes()).getStopTime().getDepartureTime());
//
//      // FIXME need to compensate for distances over the end of a trip...
//      final double distanceAlongBlock = _blockStateService.getDistanceAlongBlock(
//          blockState.getBlockInstance().getBlock(),
//          prevBlockState.getBlockLocation().getStopTimeIndex(), expSchedTime);
//      final double expectedDabDelta = distanceAlongBlock - prevDab;

      
      final double pInP3Tmp = NormalDist.density(obsDelta, distStdDev,
          dabDelta)
          / NormalDist.density(obsDelta, distStdDev, obsDelta);

      avgRes.add(pInP3Tmp);
    }
    
    return avgRes.average();
  }
}

