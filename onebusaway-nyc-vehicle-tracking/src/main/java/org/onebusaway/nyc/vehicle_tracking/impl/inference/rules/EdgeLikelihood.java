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
  private ObservationCache _observationCache;

  final private double distStdDev = 250.0;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    
    if (obs.getPreviousObservation() == null || parentState == null) {
      final SensorModelResult result = new SensorModelResult("pInProgress", 1.0);
      if (EVehiclePhase.isActiveDuringBlock(phase)) {
        result.addResultAsAnd("no previous observation/vehicle-state", 0.0);
      } else {
        result.addResultAsAnd("no previous observation/vehicle-state", 1.0);
      }
      return result;
    }
    
    final SensorModelResult result = new SensorModelResult("pInProgress", 1.0);
    final BlockState blockState = state.getBlockState();
    Set<BlockState> prevBlockStates = Sets.newHashSet();
    boolean previouslyInactive = 
        blockState == null
        || parentState.getBlockState() == null 
        || !parentState.getBlockState().getBlockInstance().equals(blockState.getBlockInstance());
    if (!previouslyInactive)
      prevBlockStates.add(context.getParentState().getBlockState());
    
    /*
     * When we have no block-state, we use the best state with a route that
     * matches the observations DSC-implied route, if any.
     */
    if (blockState == null || phase != EVehiclePhase.IN_PROGRESS) {
      final Double bestInProgressProb = _observationCache.getValueForObservation(
          obs, EObservationCacheKey.ROUTE_LOCATION);

      double invResult;

      if (bestInProgressProb == null || obs.isAtBase())
        invResult = 1.0;
      else
        invResult = 1.0 - bestInProgressProb;

      result.addResultAsAnd("pNotInProgress", invResult);
      return result;
    }

    if (prevBlockStates.isEmpty()) {
      try {

        final BestBlockStates prevBestStates = _blockStateService.getBestBlockLocations(
            obs.getPreviousObservation(), blockState.getBlockInstance(), 0,
            Double.POSITIVE_INFINITY);

        if (prevBestStates == null || prevBestStates.getAllStates().isEmpty())
//          prevBlockStates.add(_blockStateService.getAsState(blockState.getBlockInstance(), 0.0));
          return result.addResultAsAnd("pNoPrevState", 0.5);
        else
          prevBlockStates.addAll(prevBestStates.getAllStates());

      } catch (final MissingShapePointsException e) {
        e.printStackTrace();
        return result.addResultAsAnd("missing shapepoints", 0.0);
      }
    }

    /*
     * GPS Error
     */
    final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
    final ProjectedPoint p2 = obs.getPoint();

    final double d = SphericalGeometryLibrary.distance(p1.getLat(),
        p1.getLon(), p2.getLat(), p2.getLon());

    /*
     * Schedule Deviation
     */
    final long obsSchedDev = FastMath.abs((obs.getTime() - blockState.getBlockInstance().getServiceDate())/1000
        - blockState.getBlockLocation().getScheduledTime());

    /*
     * Edge Movement
     */
    final double pInP1 = 1.0 - FoldedNormalDist.cdf(10.0, 50.0, d);
    final double pInP2 = 1.0 - FoldedNormalDist.cdf(15.0, 80.0,
        obsSchedDev / 60.0);
    final double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();

    final Tally avgRes = new Tally();

    final double obsDelta = SphericalGeometryLibrary.distance(obs.getLocation(), 
        obs.getPreviousObservation().getLocation());
    /*
     * When there are multiple (potential) previously in-progress block-states,
     * we need to average over them to determine
     */
    for (final BlockState prevBlockState : prevBlockStates) {
      final double prevDab = prevBlockState.getBlockLocation().getDistanceAlongBlock();
      final double dabDelta = currentDab - prevDab;

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

    final double pInP3 = avgRes.average();
//    final double pLogInProgress = FastMath.log(pInP1) + FastMath.log(pInP2)
//        + FastMath.log(pInP3);

    result.addResultAsAnd("gps", pInP1);
    result.addResultAsAnd("schedule", pInP2);
    result.addResultAsAnd("distance", pInP3);

//    final double pInProgress = FastMath.exp(pLogInProgress);
    final double pInProgress = result.getProbability();

    /*
     * Keep the best (minimal location deviation) block state with a route
     * matching the dsc.
     */
    if (obs.getDscImpliedRouteCollections().contains(
        blockState.getBlockLocation().getActiveTrip().getTrip().getRouteCollection().getId())) {

      final Double bestInProgressProb = _observationCache.getValueForObservation(
          obs, EObservationCacheKey.ROUTE_LOCATION);

      if (bestInProgressProb == null || bestInProgressProb < pInProgress) {
        _observationCache.putValueForObservation(obs,
            EObservationCacheKey.ROUTE_LOCATION, pInProgress);
      }
    }

    return result;
  }
}
