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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// TODO FIXME update for runs!
@Component
public class BlockStateTransitionModel {

  private static Logger _log = LoggerFactory
      .getLogger(BlockStateTransitionModel.class);
  
  private DestinationSignCodeService _destinationSignCodeService;

  private BlocksFromObservationService _blocksFromObservationService;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  private ObservationCache _observationCache;

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  private ConfigurationService _configurationService;
  /****
   * Parameters
   ****/

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
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategy blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  /****
   * Block State Methods
   ****/

  /**
   * This method samples the best block transition. It should probably return
   * all best possible. Just following convention for now.
   * 
   * @param parentState
   * @param motionState
   * @param journeyState
   * @param obs
   * @return
   */
  public BlockState transitionBlockState(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    BlockState parentBlockState = parentState.getBlockState();
    Set<BlockState> potentialTransStates = null;

    EVehiclePhase parentPhase = parentState.getJourneyState().getPhase();

    boolean allowBlockChange = allowBlockTransition(parentState, motionState,
        journeyState, obs);

    /**
     * Do we explicitly allow a block change? If not, we just advance along our
     * current block.
     * 
     * Note that we are considering the parentPhase, not the newly proposed
     * phase here. I think that makes sense, since the proposed journeyPhase is
     * just an iteration over all phases with no real consideration of
     * likelihood yet.
     */
    if (!allowBlockChange) {

      if (EVehiclePhase.isActiveDuringBlock(parentPhase)) {

        if (parentBlockState == null)
          return null;

        /**
         * If we're off block, kill the block
         */
        if (_vehicleStateLibrary.isOffBlock(obs.getPreviousObservation(),
            parentBlockState))
          return null;

        /**
         * We're currently in progress on a block, so we update our block
         * location along the block that we're actively serving
         */
        potentialTransStates = advanceAlongBlock(parentPhase, parentBlockState,
            obs);

      } else {

        /**
         * We're not serving a block, so we kill the block state
         */
        return null;
      }
    }

    if (obs.isOutOfService()) {
      /**
       * Operator just went out of service so forget the block
       */
      return null;
    }

    BlockState updatedBlockState = null;

    if (potentialTransStates == null) {
      /**
       * Resample the potential blocks-in-progress when no "best" transitions
       * are found (why?)
       */
      CategoricalDist<BlockState> cdf = _blockStateSamplingStrategy
          .cdfForJourneyInProgress(obs);
      if (!cdf.canSample())
        return null;

      updatedBlockState = cdf.sample();

      // if (EVehiclePhase.isLayover(parentPhase) || obs.isAtTerminal()) {
      if (EVehiclePhase.isLayover(parentPhase)
          || VehicleStateLibrary.isAtPotentialLayoverSpot(updatedBlockState
              .getBlockLocation())) {
        updatedBlockState = _blocksFromObservationService.advanceLayoverState(
            obs.getTime(), updatedBlockState);
      }
    } else {
      /**
       * Sample the "best" possible transitions (best in terms of schedDev and
       * locDev)
       */
      CategoricalDist<BlockState> cdf = new CategoricalDist<BlockState>();

      for (BlockState state : potentialTransStates) {

        double p = _blockStateSamplingStrategy.scoreState(state, obs);

        cdf.put(p, state);
      }

      if (!cdf.canSample())
        return null;

      updatedBlockState = cdf.sample();
    }

    return updatedBlockState;
  }

  public Set<BlockState> getClosestBlockStates(BlockState blockState,
      Observation obs) {

    Map<BlockInstance, Set<BlockState>> states = _observationCache
        .getValueForObservation(obs,
            EObservationCacheKey.CLOSEST_BLOCK_LOCATION);

    if (states == null) {
      states = new HashMap<BlockInstance, Set<BlockState>>();
      _observationCache.putValueForObservation(obs,
          EObservationCacheKey.CLOSEST_BLOCK_LOCATION, states);
    }

    BlockInstance blockInstance = blockState.getBlockInstance();

    Set<BlockState> closestState = states.get(blockInstance);

    if (closestState == null) {

      closestState = getClosestBlockStateUncached(blockState, obs);

      states.put(blockInstance, closestState);
    }

    return closestState;
  }

  /****
   * Private Methods
   ****/

  // FIXME TODO this should include run info...
  private boolean allowBlockTransition(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    Observation prevObs = obs.getPreviousObservation();

    /**
     * Have we just transitioned out of a terminal?
     */
    if (prevObs != null && parentState.getBlockState() != null) {

      // If we just move out of the terminal, allow a block transition
      if (prevObs.isAtTerminal() && !obs.isAtTerminal()) {
        return true;
      }
    }

    NycRawLocationRecord record = obs.getRecord();
    String dsc = record.getDestinationSignCode();

    boolean unknownDSC = _destinationSignCodeService
        .isUnknownDestinationSignCode(dsc);

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

  private Set<BlockState> advanceAlongBlock(EVehiclePhase phase,
      BlockState blockState, Observation obs) {

    Observation prevObs = obs.getPreviousObservation();

    double distanceToTravel = 300
        + SphericalGeometryLibrary.distance(prevObs.getLocation(),
            obs.getLocation()) * _blockDistanceTravelScale;

    /**
     * TODO: Should we allow a vehicle to travel backwards?
     */
    Set<BlockState> updatedBlockStates = _blocksFromObservationService
        .advanceState(obs, blockState, 0, distanceToTravel);

    if (EVehiclePhase.isLayover(phase) || obs.isAtTerminal()) {
      for (BlockState state : updatedBlockStates) {
        state = _blocksFromObservationService.advanceLayoverState(
            obs.getTime(), state);
      }
    }

    return updatedBlockStates;
  }

  private Set<BlockState> getClosestBlockStateUncached(BlockState blockState,
      Observation obs) {

    BlockState closestState;
    double distanceToTravel = 400;

    Observation prevObs = obs.getPreviousObservation();

    if (prevObs != null) {
      distanceToTravel = Math.max(
          distanceToTravel,
          SphericalGeometryLibrary.distance(obs.getLocation(),
              prevObs.getLocation())
              * _blockDistanceTravelScale);
    }

    Set<BlockState> closestStates = new HashSet<BlockState>();

    for (BlockState state : _blocksFromObservationService.advanceState(obs,
        blockState, -distanceToTravel, distanceToTravel)) {

      ScheduledBlockLocation blockLocation = state.getBlockLocation();
      double d = SphericalGeometryLibrary.distance(blockLocation.getLocation(),
          obs.getLocation());

      if (d > 400) {
        closestStates.addAll(_blocksFromObservationService.bestStates(obs,
            state));
      } else {
        closestStates.add(state);
      }
    }

    return closestStates;
  }

  public static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    NycRawLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    NycRawLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    return !ObjectUtils.equals(previouslyObservedDsc, observedDsc);
  }

  public static class BlockStateResult {

    public BlockStateResult(BlockState blockState, boolean outOfService) {
      this.blockState = blockState;
      this.outOfService = outOfService;
    }

    public BlockState blockState;
    public boolean outOfService;
  }
}
