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

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.collect.Sets;

import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BlockStateTransitionModel {

  private static Logger _log = LoggerFactory.getLogger(BlockStateTransitionModel.class);

  private DestinationSignCodeService _destinationSignCodeService;

  private BlocksFromObservationService _blocksFromObservationService;

  private ObservationCache _observationCache;

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

  private BlockStateService _blockStateService;

  /****
   * Setters
   ****/

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
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

  /****
   * Block State Methods
   ****/

  /**
   * This method takes a parent VehicleState, parentState, and a potential
   * JourneyState, journeyState, with which a new potential
   * BlockStateObservation is sampled/determined. This is where we consider a
   * block, allow a block change, or move along a block we're already tracking.
   * 
   * @param parentState
   * @param motionState
   * @param journeyState
   * @param obs
   * @return BlockStateObservation
   */
  public Set<BlockStateObservation> transitionBlockState(
      VehicleState parentState, MotionState motionState,
      JourneyState journeyState, Observation obs) {

    final BlockStateObservation parentBlockState = parentState.getBlockStateObservation();
    final Set<BlockStateObservation> potentialTransStates = Sets.newHashSet();

    final boolean allowBlockChange = allowBlockTransition(parentState,
        motionState, journeyState, obs);

    if (parentBlockState != null) {
      advanceAlongBlock(potentialTransStates, journeyState.getPhase(),
          parentBlockState, obs);
    } else if (!EVehiclePhase.isActiveDuringBlock(journeyState.getPhase())){
      potentialTransStates.add(null);
    }

    /**
     * If this state has no transitions, consider block changes. Otherwise, pick
     * the best transition.
     */
    if (allowBlockChange) {
      potentialTransStates.addAll(_blocksFromObservationService.determinePotentialBlockStatesForObservation(
          obs));
      if (EVehiclePhase.isActiveDuringBlock(journeyState.getPhase()))
        potentialTransStates.remove(null);
    }
    
    Set<BlockStateObservation> actualTrans = Sets.newHashSet();
    for (BlockStateObservation newBlockState : potentialTransStates) {
      if (isTransitionAllowed(parentState, obs, newBlockState, journeyState))
        actualTrans.add(newBlockState);
    }

    return actualTrans;
  }

  private boolean isTransitionAllowed(VehicleState parentState,
      Observation obs, BlockStateObservation state, JourneyState journeyState) {

    EVehiclePhase phase = journeyState.getPhase();
    
    /*
     * Must be at a layover location to be in layover and
     * have served some part of block when not before.
     */
    boolean hasServedAnyPart = state != null ? 
        state.getBlockState().getBlockLocation().getDistanceAlongBlock() > 0.0 : false;
    if (EVehiclePhase.isLayover(phase)) {
      if (EVehiclePhase.LAYOVER_BEFORE == phase && hasServedAnyPart) {
        return false;
      }
      if (state == null) { 
        if (!obs.isAtTerminal())
          return false;
      } else if (!state.isAtPotentialLayoverSpot()) {
        return false;
      }
    }
    
    /*
     * Null parent states not considered from here on.
     * Allowed transitions should only permit reasonable
     * prior transitions.
     */
    if (parentState == null)
      return true;
    
    /*
     * Have to serve some part of a block and be in a layover spot
     * to have any *-during state
     */
    boolean hasServedSomePart = state != null ? 
        SensorModelSupportLibrary.hasServedSomePartOfBlock(state.getBlockState()) : false;
    if (EVehiclePhase.DEADHEAD_DURING == phase 
        || EVehiclePhase.LAYOVER_DURING == phase) {
      if (state == null
          || !hasServedSomePart
          || (EVehiclePhase.LAYOVER_DURING == phase 
            && !state.isAtPotentialLayoverSpot())) {
        return false;
      }
    }
    
    JourneyState parentJourneyState = parentState.getJourneyState();
    EVehiclePhase parentPhase = parentJourneyState.getPhase();
    
    /**
     * Transition During to Before => check things like: out of service or at base
     */
    if(!(EVehiclePhase.isActiveDuringBlock(parentPhase)
        && EVehiclePhase.isActiveBeforeBlock(phase)))
      return true;

    BlockState blockState = state != null ? state.getBlockState() : null;
    if (!(blockState != null
        && (blockState.getBlockLocation().getNextStop() == null 
          || SensorModelSupportLibrary.computeProbabilityOfEndOfBlock(blockState) > 0.9)))
      return false;

    /**
     * Added this hack to allow no block transitions after finishing one.
     */
    BlockState parentBlockState = parentState.getBlockState() != null
        ? parentState.getBlockState() : null;
    if (!(parentBlockState != null
        && (parentBlockState.getBlockLocation().getNextStop() == null 
          || SensorModelSupportLibrary.computeProbabilityOfEndOfBlock(parentBlockState) > 0.9)))
      return false;

    return true;
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

  private boolean allowBlockTransition(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    final Observation prevObs = obs.getPreviousObservation();
    final BlockStateObservation parentBlockState = parentState.getBlockStateObservation();

    if (parentBlockState == null && !obs.isOutOfService())
      return true;

    /**
     * Have we just transitioned out of a terminal?
     */
    if (prevObs != null) {
      /**
       * If we were assigned a block, then use the block's terminals, otherwise,
       * all terminals.
       */
      if (parentBlockState != null) {
        final boolean wasAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
            prevObs.getRecord(),
            parentBlockState.getBlockState().getBlockInstance());
        final boolean isAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
            obs.getRecord(),
            parentBlockState.getBlockState().getBlockInstance());

        if (wasAtBlockTerminal && !isAtBlockTerminal) {
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
   * This method searches for the best block location within a given distance
   * from the previous block location. As well, transitions to and from layover
   * states are considered here.
   * 
   * @param phase
   * @param BlockStateObservation
   * @param obs
   * @return
   */
  private void advanceAlongBlock(final Set<BlockStateObservation> results, EVehiclePhase phase,
      BlockStateObservation blockState, Observation obs) {

    Set<BlockStateObservation> updatedStates = _blocksFromObservationService.bestStates(
        obs, blockState);
    Set<BlockStateObservation> newStates = Sets.newHashSet();
    for (final BlockStateObservation updatedBlockState : updatedStates) {
      /*
       * We don't allow movements backward along a block.
       * So we hang on to our current position.
       */
//      int compRes = Double.compare(blockState.getBlockState().getBlockLocation().getDistanceAlongBlock(),
//            updatedBlockState.getBlockState().getBlockLocation().getDistanceAlongBlock());
//      if (compRes > 0) {
//        newStates.add(blockState);
//      } else {
        newStates.add(updatedBlockState);
//      }
    }
    
    
//    BlockTripEntry blockTrip = blockState.getBlockState().getBlockLocation().getActiveTrip();
//    double distAlongTrip = blockState.getBlockState().getBlockLocation().getDistanceAlongBlock()
//      - blockTrip.getDistanceAlongBlock();
//    
//    /*
//     * If the movement takes us pass the trip we were doing, then
//     * propose then next.
//     */
//    if (distTraveled + distAlongTrip > blockTrip.getTrip().getTotalTripDistance()) {
//      BlockState nextState = _blockStateService.getAsState(blockState.getBlockState().getBlockInstance(), 
//          blockTrip.getNextTrip().getDistanceAlongBlock());
//      boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
//          nextState, obs);
//      newStates.add(new BlockStateObservation(nextState, obs, isAtPotentialLayoverSpot, false));
//      
//    }
    
    if (updatedStates.isEmpty()) {
      double distTraveled = SphericalGeometryLibrary.distance(obs.getLocation(),
          obs.getPreviousObservation().getLocation());
      BlockState nextState = _blockStateService.getAsState(blockState.getBlockState().getBlockInstance(), 
          blockState.getBlockState().getBlockLocation().getDistanceAlongBlock() + distTraveled);
      boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
          nextState, obs);
      newStates.add(new BlockStateObservation(nextState, obs, isAtPotentialLayoverSpot, false));
//      newStates.add(blockState);
    }
    
    for (BlockStateObservation newState : newStates) {
      if (EVehiclePhase.isLayover(phase))
        results.add(_blocksFromObservationService.advanceLayoverState(obs, newState));
      else
        results.add(newState);
    }

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
        boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
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
