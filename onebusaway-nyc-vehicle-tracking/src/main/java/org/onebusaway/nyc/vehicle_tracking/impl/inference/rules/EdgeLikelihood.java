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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ScheduleDeviationLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;

import com.google.common.cache.Cache;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class EdgeLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;
  private ObservationCache _observationCache;

  final private double distStdDev = 250.0;

  private final LoadingCache<EdgeLikelihoodContext, SensorModelResult> _cache = 
      CacheBuilder.newBuilder()
      .concurrencyLevel(1)
      .initialCapacity(9000)
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build(
      new CacheLoader<EdgeLikelihoodContext, SensorModelResult>() {
        @Override
        public SensorModelResult load(EdgeLikelihoodContext key)
            throws Exception {
          return likelihood(key);
        }
      });
  
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
    
    EdgeLikelihoodContext edgeContext = new EdgeLikelihoodContext(context);
    return _cache.getUnchecked(edgeContext);
  }

  public SensorModelResult likelihood(EdgeLikelihoodContext context) {
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = context.getPhase();
    final SensorModelResult result = new SensorModelResult("pInProgress", 1.0);
    final BlockState blockState = context.getBlockState();
    Set<BlockState> prevBlockStates = Sets.newHashSet();
    if (context.getPreviousBlockState() != null)
      prevBlockStates.add(context.getPreviousBlockState());
    
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
          return result.addResultAsAnd("pNoPrevState", 0.0);

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
    final long obsSchedDev = (obs.getTime() - blockState.getBlockInstance().getServiceDate())/1000
        - blockState.getBlockLocation().getScheduledTime();

    /*
     * Edge Movement
     */
    final double pInP1 = 1.0 - FoldedNormalDist.cdf(10.0, 50.0, d);
    final double pInP2 = 1.0 - FoldedNormalDist.cdf(5.0, 40.0,
        obsSchedDev / 60.0);
    final double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();

    final Tally avgRes = new Tally();

    /*
     * When there are multiple (potential) previously in-progress block-states,
     * we need to average over them to determine
     */
    for (final BlockState prevBlockState : prevBlockStates) {
      final double prevDab = prevBlockState.getBlockLocation().getDistanceAlongBlock();
      final double actualDabDelta = currentDab - prevDab;

      /*
       * Use whichever comes first: previous time incremented by observed change
       * in time, or last stop departure time.
       */
      final int expSchedTime = Math.min(
          (int) (prevBlockState.getBlockLocation().getScheduledTime() 
              + (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000),
          Iterables.getLast(
              prevBlockState.getBlockInstance().getBlock().getStopTimes()).getStopTime().getDepartureTime());

      // FIXME need to compensate for distances over the end of a trip...
      final double distanceAlongBlock = _blockStateService.getDistanceAlongBlock(
          blockState.getBlockInstance().getBlock(),
          prevBlockState.getBlockLocation().getStopTimeIndex(), expSchedTime);
      final double expectedDabDelta = distanceAlongBlock - prevDab;

      final double pInP3Tmp = NormalDist.density(expectedDabDelta, distStdDev,
          actualDabDelta)
          / NormalDist.density(expectedDabDelta, distStdDev, expectedDabDelta);

      avgRes.add(pInP3Tmp);
    }

    final double pInP3 = avgRes.average();
    final double pLogInProgress = FastMath.log(pInP1) + FastMath.log(pInP2)
        + FastMath.log(pInP3);

    result.addResult("gps", pInP1);
    result.addResult("schedule", pInP2);
    result.addResult("distance", pInP3);

    final double pInProgress = FastMath.exp(pLogInProgress);

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

    result.addResultAsAnd("pInProgress", pInProgress);
    return result;
  }

  private static class EdgeLikelihoodContext {
    private final EVehiclePhase _phase;
    private final Observation _obs;
    private final BlockState _blockState;
    private final BlockState _previousBlockState;

    public EdgeLikelihoodContext(Context context) {
      _obs = context.getObservation();
      VehicleState parentState = context.getParentState();
      _phase = context.getState().getJourneyState().getPhase();
      _blockState = context.getState().getBlockState();
      boolean previouslyInactive = 
          _blockState == null
          || parentState.getBlockState() == null 
          || !parentState.getBlockState().getBlockInstance().equals(_blockState.getBlockInstance())
          || !EVehiclePhase.isActiveDuringBlock(parentState.getJourneyState().getPhase());
      if (previouslyInactive)
        _previousBlockState = null;
      else
        _previousBlockState = context.getParentState().getBlockState();
    }

    public Observation getObservation() {
      return _obs;
    }

    public EVehiclePhase getPhase() {
      return _phase;
    }

    public BlockState getBlockState() {
      return _blockState;
    }

    public BlockState getPreviousBlockState() {
      return _previousBlockState;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((_blockState == null) ? 0 : _blockState.hashCode());
      result = prime * result + ((_obs == null) ? 0 : _obs.hashCode());
      result = prime * result + ((_phase == null) ? 0 : _phase.hashCode());
      result = prime
          * result
          + ((_previousBlockState == null) ? 0 : _previousBlockState.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof EdgeLikelihoodContext)) {
        return false;
      }
      EdgeLikelihoodContext other = (EdgeLikelihoodContext) obj;
      if (_blockState == null) {
        if (other._blockState != null) {
          return false;
        }
      } else if (!_blockState.equals(other._blockState)) {
        return false;
      }
      if (_obs == null) {
        if (other._obs != null) {
          return false;
        }
      } else if (!_obs.equals(other._obs)) {
        return false;
      }
      if (_phase != other._phase) {
        return false;
      }
      if (_previousBlockState == null) {
        if (other._previousBlockState != null) {
          return false;
        }
      } else if (!_previousBlockState.equals(other._previousBlockState)) {
        return false;
      }
      return true;
    }
    
  }
}
