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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JourneyStateTransitionModel {

  /*
   * Time (seconds) that the vehicle needs to be not moving in order to be a layover.
   */
  private static final double LAYOVER_WAIT_TIME = 120d;
  
  /*
   * The minimum-required distance left on the trip to be eligible for detour state.
   */
  private static final double _remainingTripDistanceDetourCutoff = 280d;

  private VehicleStateLibrary _vehicleStateLibrary;

  private BlocksFromObservationService _blocksFromObservationService;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  public static boolean isDetour(BlockStateObservation newState,
      boolean hasSnappedStates, boolean hasNotMoved, VehicleState parentState) {

    /*
     * We only give detour to states that are supposed to be in-progress,
     * haven't just changed their run, and don't have snapped states. 
     * 
     */
    if (parentState == null || parentState.getBlockStateObservation() == null
        || newState.isSnapped() != Boolean.FALSE || hasSnappedStates
        || newState.isOnTrip() == Boolean.FALSE
        || MotionModelImpl.hasRunChanged(parentState.getBlockStateObservation(), newState))
      return false;
    
    /*
     * Also, some weird edge case came up (see trace 
     * Trace_7564_20101202T034909) where perpendicular movement was
     * occurring just at the end of a block, so that detour was possible
     * for the remaining < 50m.  Now we make sure that there's some 
     * trip remaining to consider detour.
     */ 
    final double remainingDistanceOnTrip = 
        newState.getBlockState().getBlockLocation().getActiveTrip().getTrip().getTotalTripDistance() - (
        newState.getBlockState().getBlockLocation().getDistanceAlongBlock() -
        newState.getBlockState().getBlockLocation().getActiveTrip().getDistanceAlongBlock()); 
    
    if (remainingDistanceOnTrip < _remainingTripDistanceDetourCutoff)
      return false;

    /*
     * If it was previously snapped, in-service and in the middle of a block
     * (true if this was called) and the current state is not snapped, or it was
     * in detour and it's still not snapped, then it's a detour.
     */
    if (parentState.getJourneyState().getIsDetour()
        || (parentState.getBlockStateObservation().isSnapped()
            && parentState.getBlockStateObservation().isOnTrip() 
            && parentState.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS
            && !hasNotMoved)) {
      return true;
    }

    return false;
  }

  /**
   * Determine the conditions necessary to say it's stopped at a layover.
   * 
   * @param vehicleNotMoved
   * @param timeDelta
   */
  public static boolean isLayoverStopped(boolean vehicleNotMoved,
      Observation obs, VehicleState parentState) {

    if (parentState == null || !vehicleNotMoved)
      return false;

    final long secondsNotInMotion = (obs.getTime() - parentState.getMotionState().getLastInMotionTime()) / 1000;

    if (secondsNotInMotion >= LAYOVER_WAIT_TIME)
      return true;

    return false;
  }

  /*
   * A deterministic journey state logic.<br>
   */
  public JourneyState getJourneyState(BlockStateObservation blockState,
      VehicleState parentState, Observation obs, boolean vehicleNotMoved) {

    if (_vehicleStateLibrary.isAtBase(obs.getLocation()))
      return JourneyState.atBase();

    final boolean isLayoverStopped = isLayoverStopped(vehicleNotMoved, obs,
        parentState);
    final boolean hasSnappedStates = _blocksFromObservationService.hasSnappedBlockStates(obs);

    if (blockState != null) {
      final double distanceAlong = blockState.getBlockState().getBlockLocation().getDistanceAlongBlock();
      if (distanceAlong <= 0.0) {
        if (isLayoverStopped && blockState.isAtPotentialLayoverSpot()) {
          return JourneyState.layoverBefore();
        } else {
          // TODO: handle layover-in-motion here (possibly) to allow a moving layover_before
          return JourneyState.deadheadBefore(null);
        }
      } else if (distanceAlong > blockState.getBlockState().getBlockInstance().getBlock().getTotalBlockDistance()) {
        /*
         * Note: we changed this from >= because snapped deadhead-after states could be more
         * likely than in-progress at/near the end.
         */
         return JourneyState.deadheadAfter();
        
      } else {
        /*
         * In the middle of a block.
         */
        final boolean isDetour = isDetour(blockState,
            hasSnappedStates, vehicleNotMoved, parentState);
        /*
         * It may help to remember here that if parentState == null then isLayoverStopped === null
         * That is, the below condition will always be false if parentState == null  
         */
        
        if (isLayoverStopped && blockState.isAtPotentialLayoverSpot()) {
          if ((obs.hasOutOfServiceDsc())) {
            if (blockState.isRunFormal()) {
              return JourneyState.layoverDuring(isDetour);
            }
            // we don't have confidence in this being a layover
            return JourneyState.deadheadDuring(isDetour); 
          } else {
            // in service DSC
            return JourneyState.layoverDuring(isDetour);
          }
        } else {
          if (blockState.isOnTrip()) {
            if (isDetour)
              return JourneyState.deadheadDuring(true);
            else if (obs.hasOutOfServiceDsc())
              return JourneyState.deadheadDuring(isDetour);
            else
              return adjustInProgressTransition(parentState != null ? parentState.getJourneyState().getPhase() : null, 
                  blockState, isDetour, isLayoverStopped);
          } else {
        	/* 
        	 * TODO: this is one place to handle layover-in-motion (following a stationary layover)
        	 * We don't need it in the above conditions because we don't want a layover-in-motion when
        	 * isOnTrip() (or isDetour or hasOutOfServiceDsc).
        	 */
            return JourneyState.deadheadDuring(false);
          }
        }
      }
    } else {
      return JourneyState.deadheadBefore(null);
    }
  }

  /**
   * The strict schedule-time based journey state assignment doesn't work for
   * late/early transitions to in-progess, since in-progress states that are
   * produced will be judged harshly if they're not exactly on the geometry.
   * This method works around that by assigning deadhead-before to some
   * states that would otherwise be in-progress.  Also helps for mid-trip
   * joins.
   * 
   * @param parentState
   * @param newEdge
   * @param journeyState
   * @return
   */
  private JourneyState adjustInProgressTransition(EVehiclePhase parentPhase,
      BlockStateObservation newBlockState, boolean isDetour, boolean isLayoverStopped) {

    if (parentPhase != null
        && (!parentPhase.equals(EVehiclePhase.IN_PROGRESS)
          && !parentPhase.equals(EVehiclePhase.DEADHEAD_AFTER))
        && !newBlockState.isSnapped()) {
      final boolean wasPrevStateDuring = EVehiclePhase.isActiveDuringBlock(parentPhase);
      /*
       * If it was a layover, and now it's not, then change to deadhead
       * TODO: handle layover-in-motion transition from stationary layover to a moving one
       */
      if (EVehiclePhase.isLayover(parentPhase)
          && !(isLayoverStopped && newBlockState.isAtPotentialLayoverSpot())) {
        return wasPrevStateDuring
            ? JourneyState.deadheadDuring(isDetour)
            : JourneyState.deadheadBefore(null);
      /*
       * If it wasn't a layover and now it is, become one
       */
      } else if (!EVehiclePhase.isLayover(parentPhase)
          && (isLayoverStopped && newBlockState.isAtPotentialLayoverSpot())) {
        return wasPrevStateDuring
            ? JourneyState.layoverDuring(isDetour)
            : JourneyState.layoverBefore();
      } else {
        return JourneyState.getStateForPhase(parentPhase, isDetour, null);
      }
    } else {
      return JourneyState.inProgress();
    }
  }
  
  static public boolean isLocationOnATrip(BlockState blockState) {
    return isLocationOnATrip(blockState.getBlockLocation());
  }

  static public boolean isLocationOnATrip(ScheduledBlockLocation blockLoc) {
    final double distanceAlong = blockLoc.getDistanceAlongBlock();
    final BlockTripEntry trip = blockLoc.getActiveTrip();
    final double tripDistFrom = trip.getDistanceAlongBlock();
    final double tripDistTo = tripDistFrom
        + trip.getTrip().getTotalTripDistance();

    if (tripDistFrom < distanceAlong && distanceAlong <= tripDistTo)
      return true;
    else
      return false;
  }

  static public boolean isLocationActive(BlockState blockState) {
    final double distanceAlong = blockState.getBlockLocation().getDistanceAlongBlock();

    if (0.0 < distanceAlong
        && distanceAlong < blockState.getBlockInstance().getBlock().getTotalBlockDistance())
      return true;
    else
      return false;
  }
}
