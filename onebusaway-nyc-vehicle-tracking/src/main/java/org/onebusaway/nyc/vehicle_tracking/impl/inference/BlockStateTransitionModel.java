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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
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

  private static Logger _log = LoggerFactory.getLogger(BlockStateTransitionModel.class);

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
   * This method takes a parent VehicleState, parentState, and a potential
   * JourneyState, journeyState, with which a new potential BlockState is
   * sampled/determined. This is where we consider a block, allow a block
   * change, or move along a block we're already tracking.
   * 
   * @param parentState
   * @param motionState
   * @param journeyState
   * @param obs
   * @return BlockState
   */
  public BlockState transitionBlockState(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    BlockState parentBlockState = parentState.getBlockState();
    Set<BlockState> potentialTransStates = new HashSet<BlockState>();

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

      /**
       * There isn't actually a block to this particle, so no progressing
       * possible.
       */
      if (parentBlockState == null)
        return null;

      /**
       * If we're off block, kill the block
       */
      if (_vehicleStateLibrary.isOffBlock(obs.getPreviousObservation(),
          parentBlockState))
        return null;

      if (EVehiclePhase.isActiveDuringBlock(parentPhase)
          || journeyState.getPhase() == EVehiclePhase.DEADHEAD_BEFORE) {
        potentialTransStates.addAll(advanceAlongBlock(parentPhase,
            parentBlockState, obs));
      } else {
        potentialTransStates.add(parentBlockState);
      }
    }

    // if (obs.isOutOfService()) {
    // /**
    // * Operator just went out of service so forget the block
    // */
    // return null;
    // }

    BlockState updatedBlockState = null;

    /**
     * If this state has no transitions, consider block changes. Otherwise, pick
     * the best transition.
     */
    if ((potentialTransStates == null || potentialTransStates.isEmpty())) {

      /*
       * We're pretty much following the path set out in ParticleFactory...
       */
      if (allowBlockChange && ParticleFactoryImpl.rng.nextDouble() >= 0.75) {
        /**
         * We're now considering that the driver may have been re-assigned, or
         * whatnot.
         */
        CategoricalDist<BlockState> cdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(obs);

        if (!cdf.canSample())
          return null;

        updatedBlockState = cdf.sample();

        if (EVehiclePhase.isLayover(parentPhase)
            || _vehicleStateLibrary.isAtPotentialTerminal(obs.getRecord(),
                updatedBlockState.getBlockInstance())) {
          updatedBlockState = _blocksFromObservationService.advanceLayoverState(
              obs.getTime(), updatedBlockState);
        }
      } else {
        return null;
      }

    } else {
      /**
       * When we return multiple possible transitions, choose the best in terms
       * of schedDev and locDev. TODO consider more! why not all the rules!?
       */
      if (potentialTransStates.size() == 1) {
        updatedBlockState = potentialTransStates.iterator().next();
      } else {
        CategoricalDist<BlockState> cdf = new CategoricalDist<BlockState>();

        for (BlockState state : potentialTransStates) {

          double p = _blockStateSamplingStrategy.scoreState(state, obs, false);

          cdf.put(p, state);
        }

        if (!cdf.canSample())
          return null;

        updatedBlockState = cdf.sample();
      }
    }

    return updatedBlockState;
  }

  public Set<BlockState> getClosestBlockStates(BlockState blockState,
      Observation obs) {

    Map<BlockInstance, Set<BlockState>> states = _observationCache.getValueForObservation(
        obs, EObservationCacheKey.CLOSEST_BLOCK_LOCATION);

    if (states == null) {
      states = new HashMap<BlockInstance, Set<BlockState>>();
      _observationCache.putValueForObservation(obs,
          EObservationCacheKey.CLOSEST_BLOCK_LOCATION, states);
    }

    BlockInstance blockInstance = blockState.getBlockInstance();

    Set<BlockState> closestState = states.get(blockInstance);

    if (closestState == null || closestState.isEmpty()) {

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
    BlockState parentBlockState = parentState.getBlockState();

    /**
     * Have we just transitioned out of a terminal?
     */
    if (prevObs != null) {
      /**
       * If we were assigned a block, then use the block's terminals, otherwise,
       * all terminals.
       */
      if (parentBlockState == null) {
        if (prevObs.isAtTerminal() && !obs.isAtTerminal())
          return true;
      } else if (parentBlockState != null) {
        boolean wasAtBlockTerminal = _vehicleStateLibrary.isAtPotentialTerminal(
            prevObs.getRecord(), parentBlockState.getBlockInstance());
        boolean isAtBlockTerminal = _vehicleStateLibrary.isAtPotentialTerminal(
            obs.getRecord(), parentBlockState.getBlockInstance());

        if (wasAtBlockTerminal && !isAtBlockTerminal) {
          return true;
        }
      }
    }

    NycRawLocationRecord record = obs.getRecord();
    String dsc = record.getDestinationSignCode();

    boolean unknownDSC = _destinationSignCodeService.isUnknownDestinationSignCode(dsc);

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

    if (!obs.isOutOfService() && parentBlockState == null)
      return true;

    return false;
  }

  /**
   * This method searches for the best block location within a given distance
   * from the previous block location. As well, transitions to and from layover
   * states are considered here.
   * 
   * @param phase
   * @param blockState
   * @param obs
   * @return
   */
  private Set<BlockState> advanceAlongBlock(EVehiclePhase phase,
      BlockState blockState, Observation obs) {

    Observation prevObs = obs.getPreviousObservation();

    double distanceToTravel = 300
        + SphericalGeometryLibrary.distance(prevObs.getLocation(),
            obs.getLocation()) * _blockDistanceTravelScale;

    /**
     * TODO: Should we allow a vehicle to travel backwards?
     */
    Set<BlockState> updatedBlockStates = _blocksFromObservationService.advanceState(
        obs, blockState, 0, distanceToTravel);

    if (updatedBlockStates != null
        && (EVehiclePhase.isLayover(phase) || _vehicleStateLibrary.isAtPotentialTerminal(
            obs.getRecord(), blockState.getBlockInstance()))) {
      for (BlockState state : updatedBlockStates) {
        state = _blocksFromObservationService.advanceLayoverState(
            obs.getTime(), state);
      }
    }

    return updatedBlockStates;
  }

  private Set<BlockState> getClosestBlockStateUncached(BlockState blockState,
      Observation obs) {

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
    Set<BlockState> advancedStates = _blocksFromObservationService.advanceState(
        obs, blockState, -distanceToTravel, distanceToTravel);

    if (advancedStates == null)
      return closestStates;

    for (BlockState state : advancedStates) {

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

    /*
     * keep these flags alive TODO must be a better/more consistent way
     */
    if (!closestStates.isEmpty()) {
      for (BlockState state : closestStates) {
        state.setOpAssigned(blockState.getOpAssigned());
        state.setRunReported(blockState.getRunReported());
        state.setRunReportedAssignedMismatch(blockState.isRunReportedAssignedMismatch());
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
