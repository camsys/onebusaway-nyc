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
public class GpsLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;

  final static public double inProgressGpsStdDev = 50.0;

  private static final double deadheadBeforeGpsStdDevScale = 1.0;
  final private double inProgressGpsMean = 10.0;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();
    final BlockState blockState = state.getBlockState();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    
    SensorModelResult result = new SensorModelResult("pGps", 1.0);
    
    if (blockState == null) {
      final double pGps = FoldedNormalDist.density(inProgressGpsMean, inProgressGpsStdDev, inProgressGpsMean);
      result.addResultAsAnd("gps(no state)", 0.5*pGps);
      
    } else if (EVehiclePhase.AT_BASE == phase) {
      
      result.addResultAsAnd("gps(at-base)", 1.0);
      
    } else if (EVehiclePhase.IN_PROGRESS == phase) {
      
      final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
      final ProjectedPoint p2 = obs.getPoint();
      final double d = SphericalGeometryLibrary.distance(p1.getLat(),
          p1.getLon(), p2.getLat(), p2.getLon());
      final double pGps = FoldedNormalDist.density(inProgressGpsMean, inProgressGpsStdDev, d);
      result.addResultAsAnd("gps(in-progress)", pGps);
      
    } else if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
      
      final double pGps = computeBeforeGpsProb(blockState, obs);
      result.addResultAsAnd("gps(deadhead-before)", pGps);
      
    } else if (EVehiclePhase.LAYOVER_BEFORE == phase) {
      
      final double pGps = computeBeforeGpsProb(blockState, obs);
      result.addResultAsAnd("gps(layover-before)", pGps);
      
    } else if (EVehiclePhase.DEADHEAD_DURING == phase) {
      
      final CoordinatePoint pNextStop = blockState.getBlockLocation().getNextStop()
          .getStopTime().getStop().getStopLocation();
      final double expectedDistFromStop = blockState.getBlockLocation().getNextStop().getDistanceAlongBlock()
          - blockState.getBlockLocation().getDistanceAlongBlock();
      final CoordinatePoint pNow = obs.getLocation();
      final double obsDistToStop = SphericalGeometryLibrary.distance(pNow, pNextStop);
      final double pGps = FoldedNormalDist.density(expectedDistFromStop, inProgressGpsStdDev, obsDistToStop);
      result.addResultAsAnd("gps(deadhead-during)", pGps);
      
    } else if (EVehiclePhase.LAYOVER_DURING == phase) {
      
      final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
      final ProjectedPoint p2 = obs.getPoint();
      final double d = SphericalGeometryLibrary.distance(p1.getLat(),
          p1.getLon(), p2.getLat(), p2.getLon());
      final double pGps = FoldedNormalDist.density(inProgressGpsMean, inProgressGpsStdDev, d);
      result.addResultAsAnd("gps(layover-during)", pGps);
      
    }
    
    return result;
  }
  
  private double computeBeforeGpsProb(BlockState blockState, Observation obs) {
    /*
     * Check if we're inching toward the start
     */
    final CoordinatePoint pDest = blockState.getBlockLocation().getLocation();
    final CoordinatePoint pNow = obs.getLocation();
    
    final double pGps;
    if (obs.getPreviousObservation() != null) {
      final CoordinatePoint pLast= obs.getPreviousObservation().getLocation();
      
      final double distLast = SphericalGeometryLibrary.distance(pDest, pLast);
      final double distNow = SphericalGeometryLibrary.distance(pDest, pNow);
      pGps = FoldedNormalDist.density(0.0, 
          distLast * deadheadBeforeGpsStdDevScale, distLast - distNow);
    } else {
      final double distNow = SphericalGeometryLibrary.distance(pDest, pNow);
      pGps = FoldedNormalDist.density(0.0, 
          distNow * deadheadBeforeGpsStdDevScale, distNow);
    }
    
    return pGps;
  }
}

