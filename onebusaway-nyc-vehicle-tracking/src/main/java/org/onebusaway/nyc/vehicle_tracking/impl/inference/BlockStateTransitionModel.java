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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ObjectUtils;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockStateTransitionModel {
	
  private DestinationSignCodeService _destinationSignCodeService;

  private BlocksFromObservationService _blocksFromObservationService;

  private ObservationCache _observationCache;

  /****
   * Parameters
   ****/

  static class LocalRandom extends ThreadLocal<Random> {
    long _seed = 0;

    LocalRandom(long seed) {
      _seed = seed;
    }

    @Override
    protected Random initialValue() {
      if (_seed != 0)
        return new Random(_seed);
      else
        return new Random();
    }
  }

  static class LocalRandomDummy extends ThreadLocal<Random> {
    private static Random rng;

    LocalRandomDummy(long seed) {
      if (seed != 0)
        rng = new Random(seed);
      else
        rng = new Random();
    }

    @Override
    synchronized public Random get() {
      return rng;
    }
  }

  static ThreadLocal<Random> threadLocalRng;
  static {
    if (!ParticleFilter.getReproducibilityEnabled()) {
      threadLocalRng = new LocalRandom(0);
    } else {
      threadLocalRng = new LocalRandomDummy(0);

    }
  }

  synchronized public static void setSeed(long seed) {
    if (!ParticleFilter.getReproducibilityEnabled()) {
      threadLocalRng = new LocalRandom(seed);
    } else {
      threadLocalRng = new LocalRandomDummy(seed);

    }
  }

  /**
   * Given a potential distance to travel in physical / street-network space, a
   * fudge factor that determines how far ahead we will look along the block of
   * trips in distance to travel
   */
  private double _blockDistanceTravelScale = 1.5;

  /****
   * Setters
   ****/

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  public Set<BlockStateObservation> getClosestBlockStates(
      BlockState blockState, Observation obs) {

    Map<BlockInstance, Set<BlockStateObservation>> states = _observationCache.getValueForObservation(
        obs, EObservationCacheKey.CLOSEST_BLOCK_LOCATION);

    if (states == null) {
      states = new ConcurrentHashMap<BlockInstance, Set<BlockStateObservation>>();
      _observationCache.putValueForObservation(obs,
          EObservationCacheKey.CLOSEST_BLOCK_LOCATION, states);
    }

    final BlockInstance blockInstance = blockState.getBlockInstance();

    Set<BlockStateObservation> closestState = states.get(blockInstance);

    if (closestState == null) {

      closestState = getClosestBlockStateUncached(blockState, obs);

      states.put(blockInstance, closestState);
    }

    return closestState;
  }

  /****
   * Private Methods
   ****/

  public boolean allowBlockTransition(VehicleState parentState, Observation obs) {

    final BlockStateObservation parentBlockState = parentState.getBlockStateObservation();

    if (parentBlockState == null
        && (!obs.hasOutOfServiceDsc() && obs.hasValidDsc()))
      return true;

    if (parentBlockState != null) {
      if (parentBlockState.getRunReported() == null
          && parentBlockState.getOpAssigned() == null) {
        /*
         * We have no run information, so we will allow a run transition when we
         * hit deadhead-after.
         */
        if (parentState.getJourneyState().getPhase() == EVehiclePhase.DEADHEAD_AFTER) {
          return true;
        }
      }
    }

    final NycRawLocationRecord record = obs.getRecord();
    final String dsc = record.getDestinationSignCode();

    final boolean unknownDSC = _destinationSignCodeService.isUnknownDestinationSignCode(dsc);

    /**
     * If we have an unknown DSC, we assume it's a typo, so we assume our
     * current block will work for now. What about "0000"? 0000 is no longer
     * considered an unknown DSC, so it should pass by the unknown DSC check,
     * and to the next check, where we only allow a block change if we've
     * changed from a good DSC.
     */
    if (unknownDSC)
      return false;

    /**
     * If the destination sign code has changed, we allow a block transition
     */
    if (hasDestinationSignCodeChangedBetweenObservations(obs))
      return true;

    return false;
  }

  /**
   * Returns closest block state, spatially, to the passed
   * BlockStateObservation.
   * 
   * @param blockStateObservation
   * @param obs
   * @return
   */
  private Set<BlockStateObservation> getClosestBlockStateUncached(
      BlockState blockState, Observation obs) {

    double distanceToTravel = 400;

    final Observation prevObs = obs.getPreviousObservation();

    if (prevObs != null) {
      distanceToTravel = Math.max(
          distanceToTravel,
          SphericalGeometryLibrary.distance(obs.getLocation(),
              prevObs.getLocation())
              * _blockDistanceTravelScale);
    }

    final Set<BlockStateObservation> results = new HashSet<BlockStateObservation>();
    for (final BlockStateObservation bso : _blocksFromObservationService.advanceState(
        obs, blockState, -distanceToTravel, distanceToTravel)) {

      final ScheduledBlockLocation blockLocation = bso.getBlockState().getBlockLocation();
      final double d = SphericalGeometryLibrary.distance(
          blockLocation.getLocation(), obs.getLocation());

      if (d > 400) {
        final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            blockState, obs);
        final BlockStateObservation blockStateObservation = new BlockStateObservation(
            blockState, obs, isAtPotentialLayoverSpot, true);
        results.addAll(_blocksFromObservationService.bestStates(obs,
            blockStateObservation));
      } else {
        results.add(bso);
      }
    }

    return results;
  }

  public static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    final NycRawLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    final NycRawLocationRecord record = obs.getRecord();
    final String observedDsc = record.getDestinationSignCode();

    return !ObjectUtils.equals(previouslyObservedDsc, observedDsc);
  }

  public static class BlockStateResult {

    public BlockStateResult(BlockStateObservation BlockStateObservation,
        boolean outOfService) {
      this.BlockStateObservation = BlockStateObservation;
      this.outOfService = outOfService;
    }

    public BlockStateObservation BlockStateObservation;
    public boolean outOfService;
  }

}
