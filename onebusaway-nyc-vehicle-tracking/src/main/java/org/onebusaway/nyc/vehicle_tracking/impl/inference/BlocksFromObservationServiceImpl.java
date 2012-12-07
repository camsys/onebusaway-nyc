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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Component
public class BlocksFromObservationServiceImpl implements
    BlocksFromObservationService {

  private static Logger _log = LoggerFactory.getLogger(BlocksFromObservationServiceImpl.class);

  /**
   * Search window for active trips: slop before the start of the trip (ms).
   */
  private long _tripSearchTimeBeforeFirstStop = 60 * 60 * 1000;

  /**
   * Search window for active trips: slop after the end of the trip (ms).
   */
  private long _tripSearchTimeAfterLastStop = 30 * 60 * 1000;

  private ObservationCache _observationCache;

  private TransitGraphDao _transitGraphDao;

  private BlockCalendarService _activeCalendarService;

  private DestinationSignCodeService _destinationSignCodeService;

  private BlockStateService _blockStateService;

  private RunService _runService;

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }
  
  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setActiveCalendarService(
      BlockCalendarService activeCalendarService) {
    _activeCalendarService = activeCalendarService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  public void setTripSearchTimeBeforeFirstStop(long tripSearchTimeBeforeFirstStop) {
	_tripSearchTimeBeforeFirstStop = tripSearchTimeBeforeFirstStop;
  }

  public void setTripSearchTimeAfterLastStop(long tripSearchTimeAfteLastStop) {
	_tripSearchTimeAfterLastStop = tripSearchTimeAfteLastStop;
  }

  /****
   * {@link BlocksFromObservationService} Interface Returns "initialized"-only
   * BlockStates for an observation.
   * 
   ****/
  @Override
  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation, boolean bestBlockLocation) {

    synchronized (observation) {
      final EObservationCacheKey thisKey = bestBlockLocation
          ? EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK
          : EObservationCacheKey.JOURNEY_START_BLOCK;
  
      Set<BlockStateObservation> potentialBlockStates = _observationCache.getValueForObservation(
          observation, thisKey);
  
      if (potentialBlockStates == null) {
        unCachedDeterminePotentialBlockStatesForObservation(observation);
        potentialBlockStates = _observationCache.getValueForObservation(
            observation, thisKey);
      }
  
      return potentialBlockStates;
    }
  }

  /**
   * Returns the support for the current observation.<br>
   * This includes
   * <ul>
   * <li>"Snapped", in-progress blocks.
   * <li>DSC and run-id inferred blocks (location is 0 meters along the block,
   * i.e. start of the block).
   * <li>Empty-block vehicle state (when none of the above are found).
   * </ul>
   * 
   * @param observation
   * @return
   */
  @Override
  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation) {

    synchronized(observation) {
      final Set<BlockStateObservation> potentialBlockStates;
  
      Set<BlockStateObservation> inProgressStates = _observationCache.getValueForObservation(
          observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK);
      final Set<BlockStateObservation> notInProgressStates = _observationCache.getValueForObservation(
          observation, EObservationCacheKey.JOURNEY_START_BLOCK);
  
      if (inProgressStates == null || notInProgressStates == null) {
        potentialBlockStates = unCachedDeterminePotentialBlockStatesForObservation(observation);
        inProgressStates = _observationCache.getValueForObservation(observation,
            EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK);
      } else {
        potentialBlockStates = Sets.newHashSet(Iterables.concat(inProgressStates,
            notInProgressStates));
      }
      if (inProgressStates == null || inProgressStates.isEmpty()
          || potentialBlockStates.isEmpty())
        potentialBlockStates.add(null);
  
      return potentialBlockStates;
    } 
  }

  private Set<BlockStateObservation> unCachedDeterminePotentialBlockStatesForObservation(
      Observation observation) {

    synchronized (observation) {
      final Set<BlockStateObservation> potentialBlockStatesInProgress = Sets.newHashSet();
      final Set<BlockStateObservation> potentialBlockStatesNotInProgress = Sets.newHashSet();
      final Set<BlockInstance> snappedBlocks = Sets.newHashSet();
  
      if (observation.getRunResults().hasRunResults()
          || observation.hasValidDsc()) {
        for (final BlockState bs : _blockStateService.getBlockStatesForObservation(observation)) {
          final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
              bs, observation);
          potentialBlockStatesInProgress.add(new BlockStateObservation(bs,
              observation, isAtPotentialLayoverSpot, true));
          snappedBlocks.add(bs.getBlockInstance());
        }
  
        _observationCache.putValueForObservation(observation,
            EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK,
            potentialBlockStatesInProgress);
      }
  
      /*
       * now, use dsc, run-id and whatnot to determine blocks potentially being
       * started (i.e. 0 distance-along-block).
       */
      final Set<BlockInstance> notSnappedBlocks = Sets.newHashSet();
      determinePotentialBlocksForObservation(observation, notSnappedBlocks);

      for (final BlockInstance thisBIS : notSnappedBlocks) {
        final Set<BlockStateObservation> states = new HashSet<BlockStateObservation>();
        try {
          final BlockState bs = _blockStateService.getAsState(thisBIS, 0.0);
          final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
              bs, observation);
          states.add(new BlockStateObservation(bs, observation,
              isAtPotentialLayoverSpot, false));
        } catch (final Exception e) {
          _log.warn(e.getMessage());
          continue;
        }
        potentialBlockStatesNotInProgress.addAll(states);
      }
  
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.JOURNEY_START_BLOCK,
          potentialBlockStatesNotInProgress);
  
      return Sets.newHashSet(Iterables.concat(potentialBlockStatesInProgress,
          potentialBlockStatesNotInProgress));
    }

  }

  @Override
  public BlockStateObservation getBlockStateObservationFromDist(
      Observation obs, BlockInstance blockInstance, double distanceAlong) {
    final BlockState bs = _blockStateService.getAsState(blockInstance,
        distanceAlong);
    final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
        bs, obs);
    return new BlockStateObservation(bs, obs, isAtPotentialLayoverSpot, false);
  }

  /****
   * {@link BlocksFromObservationService} Interface
   ****/

  @Override
  public void determinePotentialBlocksForObservation(Observation observation,
      Set<BlockInstance> potentialBlocks) {

    /**
     * Second source of trips: the destination sign code
     */
    if (observation.getLastValidDestinationSignCode() != null) {
      computePotentialBlocksFromDestinationSignCodeAndRunId(observation,
          potentialBlocks);
    }

  }

  @Override
  public Set<BlockStateObservation> advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel) {

    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockState.getBlockInstance(), 0, Double.POSITIVE_INFINITY);
    } catch (final MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }

    final Set<BlockStateObservation> resStates;
    if (foundStates != null) {
      final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
      final double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

      final double maxDab = currentDistanceAlongBlock + maxDistanceToTravel;
      final double minDab = currentDistanceAlongBlock + minDistanceToTravel;
      resStates = Sets.newHashSet();

      for (final BlockState bs : foundStates.getAllStates()) {
        final double dab = bs.getBlockLocation().getDistanceAlongBlock();
        if (dab < maxDab && dab > minDab) {
          final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
              bs, observation);
          resStates.add(new BlockStateObservation(bs, observation,
              isAtPotentialLayoverSpot, true));
        }
      }
    } else {
      resStates = Collections.emptySet();
    }

    return resStates;
  }

  @Override
  public BlockStateObservation advanceLayoverState(Observation obs,
      BlockStateObservation blockState) {

    final long timestamp = obs.getTime();
    final BlockInstance instance = blockState.getBlockState().getBlockInstance();

    /**
     * The targetScheduleTime is the schedule time we SHOULD be at
     */
    int targetScheduleTime = (int) ((timestamp - instance.getServiceDate()) / 1000);

    final ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();

    final int scheduledTime = blockLocation.getScheduledTime();

    /**
     * First, we must actually be in a layover location
     */
    final BlockStopTimeEntry layoverSpot = VehicleStateLibrary.getPotentialLayoverSpot(blockLocation);

    if (layoverSpot == null)
      return blockState;

    /**
     * The layover spot is the location between the last stop in the previous
     * trip and the first stop of the next trip
     */
    final StopTimeEntry stopTime = layoverSpot.getStopTime();

    /**
     * We only adjust our layover schedule time if our current location is
     * within the range of the layover bounds.
     */
    if (scheduledTime > stopTime.getDepartureTime())
      return blockState;

    /**
     * We won't advance the vehicle out of the layover, so we put an upper bound
     * on our target schedule time
     */
    targetScheduleTime = Math.min(targetScheduleTime,
        stopTime.getDepartureTime());

    if (layoverSpot.getBlockSequence() > 0) {
      final BlockConfigurationEntry blockConfig = instance.getBlock();
      final int minArrivalTime = blockConfig.getArrivalTimeForIndex(layoverSpot.getBlockSequence() - 1);
      if (scheduledTime + 5 * 60 < minArrivalTime)
        return blockState;

      /**
       * We won't advance the vehicle out of the layover, so we put a lower
       * bound on our target schedule time
       */
      targetScheduleTime = Math.max(targetScheduleTime, minArrivalTime);
    }

    final BlockState bs = _blockStateService.getScheduledTimeAsState(instance,
        targetScheduleTime);
    final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
        bs, obs);
    final BlockStateObservation state = new BlockStateObservation(bs, obs,
        isAtPotentialLayoverSpot, true);

    return state;
  }

  /**
   * Finds the best block state assignments along the ENTIRE length of the block
   * (potentially expensive operation)
   */
  @Override
  public Set<BlockStateObservation> bestStates(Observation observation,
      BlockStateObservation blockState) {

    final BlockInstance blockInstance = blockState.getBlockState().getBlockInstance();
    blockInstance.getBlock();

    final Set<BlockStateObservation> resStates = new HashSet<BlockStateObservation>();
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockInstance, 0, Double.POSITIVE_INFINITY);
    } catch (final MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }
    if (foundStates != null) {
      for (final BlockState bs : foundStates.getAllStates()) {
        final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            bs, observation);
        resStates.add(new BlockStateObservation(bs, observation,
            isAtPotentialLayoverSpot, true));
      }
    }
    return Sets.newHashSet(resStates);
  }

  /*****
   * Private Methods
   ****/

  private void computePotentialBlocksFromDestinationSignCodeAndRunId(
      Observation observation, Set<BlockInstance> potentialBlocks) {

    final long time = observation.getTime();

    /**
     * We use the last valid DSC, which will be the current DSC if it's not 0000
     * or the most recent good DSC otherwise
     */
    final String dsc = observation.getLastValidDestinationSignCode();

    /**
     * Step 1: Figure out the set of all possible trip ids given the destination
     * sign code
     */
    final List<AgencyAndId> tripIds = Lists.newArrayList();
    if (observation.getLastValidDestinationSignCode() != null
        && !observation.hasOutOfServiceDsc() && observation.hasValidDsc())
      tripIds.addAll(_destinationSignCodeService.getTripIdsForDestinationSignCode(dsc));

    final List<String> runIds = Lists.newArrayList();
    runIds.add(observation.getOpAssignedRunId());
    runIds.addAll(observation.getBestFuzzyRunIds());

    for (final String runId : runIds) {
      tripIds.addAll(_runService.getTripIdsForRunId(runId));
    }

    if (tripIds.isEmpty()) {
      _log.info("no trips found for dsc: " + dsc);
      return;
    }

    /**
     * Step 2: Figure out which trips are actively running at the specified
     * time. We do that by looking at the range of stop times for each trip +/-
     * some extra buffer time to deal with the fact that it takes time for the
     * bus to get from the base to the first stop, and from the last stop back
     * to the base.
     */

    /**
     * Slight wrinkle: instead of expanding the stop time interval with the
     * from-base + to+base fudge factors, we just expand the current search time
     * interval instead.
     */
    final long timeFrom = time - _tripSearchTimeAfterLastStop;
    final long timeTo = time + _tripSearchTimeBeforeFirstStop;

    for (final AgencyAndId tripId : tripIds) {

      /**
       * Only consider a trip if it exists and has stop times
       */
      final TripEntry trip = _transitGraphDao.getTripEntryForId(tripId);
      if (trip == null)
        continue;

      // TODO : Are we getting back more than we want as blocks will be active
      // longer than their corresponding trips?
      final List<BlockInstance> instances = _activeCalendarService.getActiveBlocks(
          trip.getBlock().getId(), timeFrom, timeTo);
      potentialBlocks.addAll(instances);
    }

  }

  @Override
  public boolean hasSnappedBlockStates(Observation obs) {
    final Set<BlockState> blockStates = _blockStateService.getBlockStatesForObservation(obs);
    if (!blockStates.isEmpty())
      return true;
    else
      return false;
  }

  @Override
  public Set<BlockStateObservation> getSnappedBlockStates(Observation obs) {
    final Set<BlockState> blockStates = _blockStateService.getBlockStatesForObservation(obs);
    final Set<BlockStateObservation> results = Sets.newHashSet();
    for (final BlockState blockState : blockStates) {
      final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
          blockState, obs);
      results.add(new BlockStateObservation(blockState, obs,
          isAtPotentialLayoverSpot, true));
    }
    return results;
  }

  @Override
  public BlockStateObservation getBlockStateObservationFromTime(
      Observation obs, BlockInstance blockInstance, int newSchedTime) {
    final BlockState blockState = _blockStateService.getScheduledTimeAsState(
        blockInstance, newSchedTime);
    final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
        blockState, obs);
    return new BlockStateObservation(blockState, obs, isAtPotentialLayoverSpot,
        false);
  }

}
