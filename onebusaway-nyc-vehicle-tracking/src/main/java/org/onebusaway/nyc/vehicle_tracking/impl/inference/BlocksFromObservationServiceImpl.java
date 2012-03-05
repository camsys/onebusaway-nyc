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

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.impl.blocks.BlockSequence;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.RouteService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexFactoryService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockSequenceIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ServiceIntervalBlock;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BlocksFromObservationServiceImpl implements
    BlocksFromObservationService {

  private static Logger _log = LoggerFactory.getLogger(BlocksFromObservationServiceImpl.class);

  private TransitGraphDao _transitGraphDao;

  private BlockCalendarService _activeCalendarService;

  private DestinationSignCodeService _destinationSignCodeService;

  private BlockIndexFactoryService _blockIndexFactoryService;

  private BlockStateService _blockStateService;

  private BlockIndexService _blockIndexService;

  private VehicleAssignmentService _vehicleAssignmentService;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  private BlockCalendarService _blockCalendarService;

  private RouteService _routeService;

  private BlockGeospatialService _blockGeospatialService;

  private ExtendedCalendarService _calendarService;

  @Autowired
  public void setCalendarService(ExtendedCalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setRouteService(RouteService routeService) {
    _routeService = routeService;
  }

  @Autowired
  public void setBlockGeospatialService(
      BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
  }

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setOperatorAssignmentService(
      OperatorAssignmentService operatorAssignmentService) {
    _operatorAssignmentService = operatorAssignmentService;
  }

  /**
   * Default is 800 meters
   */
  private double _tripSearchRadius = 800;

  /**
   * Default is 50 minutes
   */
  private long _tripSearchTimeBeforeFirstStop = 60 * 60 * 1000;

  /**
   * Default is 30 minutes
   */
  private long _tripSearchTimeAfterLastStop = 30 * 60 * 1000;

  private boolean _includeNearbyBlocks = false;

  private ObservationCache _observationCache;

  /****
   * Public Methods
   ****/

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
  public void setBlockIndexFactoryService(
      BlockIndexFactoryService blockIndexFactoryService) {
    _blockIndexFactoryService = blockIndexFactoryService;
  }

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Autowired
  public void setVehicleAssignmentService(
      VehicleAssignmentService vehicleAssignmentService) {
    _vehicleAssignmentService = vehicleAssignmentService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  /**
   * 
   * @param tripSearchRadius in meters
   */
  public void setTripSearchRadius(double tripSearchRadius) {
    _tripSearchRadius = tripSearchRadius;
  }

  public void setTripSearchTimeBeforeFirstStop(
      long tripSearchTimeBeforeFirstStop) {
    _tripSearchTimeBeforeFirstStop = tripSearchTimeBeforeFirstStop;
  }

  public void setTripSearchTimeAfterLastStop(long tripSearchTimeAfteLastStop) {
    _tripSearchTimeAfterLastStop = tripSearchTimeAfteLastStop;
  }

  public void setIncludeNearbyBlocks(boolean includeNearbyBlocks) {
    _includeNearbyBlocks = includeNearbyBlocks;
  }
  
  /****
   * {@link BlocksFromObservationService} Interface Returns "initialized"-only
   * BlockStates for an observation.
   * 
   ****/
  @Override
  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation, boolean bestBlockLocation) {
    
    final EObservationCacheKey thisKey = bestBlockLocation
        ? EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK
        : EObservationCacheKey.JOURNEY_START_BLOCK;
    
    Set<BlockStateObservation> potentialBlockStates= _observationCache.getValueForObservation(
        observation, thisKey);

    if (potentialBlockStates== null) {
      unCachedDeterminePotentialBlockStatesForObservation(observation);
      potentialBlockStates= _observationCache.getValueForObservation(
          observation, thisKey);
    }
    
    return potentialBlockStates;
  }

  /**
   * Returns the support for the current observation.<br>
   * This includes
   * <ul>
   *  <li> "Snapped", in-progress blocks.
   *  <li> DSC and run-id inferred blocks (location is 0 meters along the block, 
   *    i.e. start of the block).
   *  <li> Empty-block vehicle state (when none of the above are found).
   * </ul>
   * 
   * @param observation
   * @return
   */
  @Override
  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation) {
    
    final Set<BlockStateObservation> potentialBlockStates;
    
    Set<BlockStateObservation> inProgressStates = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK);
    Set<BlockStateObservation> notInProgressStates = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_START_BLOCK);

    if (inProgressStates == null || notInProgressStates == null) {
      potentialBlockStates = unCachedDeterminePotentialBlockStatesForObservation(observation);
      inProgressStates = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK);
    } else {
      potentialBlockStates = Sets.newHashSet(Iterables.concat(inProgressStates, notInProgressStates));
    }
    
    if (potentialBlockStates.isEmpty() 
        || inProgressStates == null
        || inProgressStates.isEmpty())
      potentialBlockStates.add(null);
    
    return potentialBlockStates;
    
  }
  
  private Set<BlockStateObservation> unCachedDeterminePotentialBlockStatesForObservation(
      Observation observation) {

    final Set<BlockStateObservation> potentialBlockStatesInProgress = Sets.newHashSet();
    final Set<BlockStateObservation> potentialBlockStatesNotInProgress = Sets.newHashSet();
    final Set<BlockInstance> snappedBlocks = Sets.newHashSet();

    if (!observation.isOutOfService()) {
      for (final BlockState bs : _blockStateService.getBlockStatesForObservation(observation)) {
        boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            bs, observation);
        potentialBlockStatesInProgress.add(new BlockStateObservation(bs, observation, 
            isAtPotentialLayoverSpot, true));
        snappedBlocks.add(bs.getBlockInstance());
      }

      _observationCache.putValueForObservation(observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK,
          potentialBlockStatesInProgress);
    }

    /*
     * now, use dsc, run-id and whatnot to determine blocks potentially being
     * started (i.e. 0 distance-along-block).
     */
    final Set<BlockInstance> notSnappedBlocks = Sets.newHashSet();
    determinePotentialBlocksForObservation(observation, notSnappedBlocks);
    notSnappedBlocks.removeAll(snappedBlocks);
    for (final BlockInstance thisBIS : notSnappedBlocks) {
      final Set<BlockStateObservation> states = new HashSet<BlockStateObservation>();
      try {
        BlockState bs = _blockStateService.getAsState(thisBIS, 0.0);
        boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            bs, observation);
        states.add(new BlockStateObservation(bs, observation, isAtPotentialLayoverSpot, false));
      } catch (final Exception e) {
        _log.warn(e.getMessage());
        continue;
      }
      potentialBlockStatesNotInProgress.addAll(states);
    }

    _observationCache.putValueForObservation(observation, EObservationCacheKey.JOURNEY_START_BLOCK,
        potentialBlockStatesNotInProgress);

    return Sets.newHashSet(Iterables.concat(potentialBlockStatesInProgress, potentialBlockStatesNotInProgress));

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

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    final double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

    final Set<BlockStateObservation> resStates = new HashSet<BlockStateObservation>();
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockState.getBlockInstance(), currentDistanceAlongBlock
              + minDistanceToTravel, currentDistanceAlongBlock
              + maxDistanceToTravel);
    } catch (final MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }

    if (foundStates != null) {
      for (final BlockState bs : foundStates.getAllStates()) {
        boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            bs, observation);
        resStates.add(new BlockStateObservation(bs, observation, isAtPotentialLayoverSpot, true));
      }
    }

    return Sets.newHashSet(resStates);
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

    BlockState bs = _blockStateService.getScheduledTimeAsState(instance, targetScheduleTime);
    boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
        bs, obs);
    final BlockStateObservation state = new BlockStateObservation(bs, obs, isAtPotentialLayoverSpot, true);

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
        boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(
            bs, observation);
        resStates.add(new BlockStateObservation(bs, observation, isAtPotentialLayoverSpot, true));
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
        && !observation.isOutOfService())
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

//  private Set<BlockTripEntry> computeNearbyBlocks(Observation observation,
//      Set<BlockInstance> potentialBlocks) {
//
//    final NycRawLocationRecord record = observation.getRecord();
//    final long time = observation.getTime();
//    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
//    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);
//    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
//        record.getLatitude(), record.getLongitude(), _tripSearchRadius);
//
//    final Set<BlockSequenceIndex> blockSequenceIdxs = _blockGeospatialService.getBlockSequenceIndexPassingThroughBounds(bounds);
//    final Set<BlockTripEntry> blockTripsForObs = new HashSet<BlockTripEntry>();
//
//    for (final BlockSequenceIndex blockSequenceIdx : blockSequenceIdxs) {
//      final ServiceIntervalBlock intervals = blockSequenceIdx.getServiceIntervalBlock();
//      final ServiceInterval range = intervals.getRange();
//      final Collection<Date> serviceDates = _calendarService.getServiceDatesWithinRange(
//          blockSequenceIdx.getServiceIds(), range, timeFrom, timeTo);
//      if (serviceDates.isEmpty())
//        continue;
//
//      for (final Date serviceDate : serviceDates) {
//        final int scheduledTimeFrom = (int) ((timeFrom.getTime() - serviceDate.getTime()) / 1000);
//        final int scheduledTimeTo = (int) ((timeTo.getTime() - serviceDate.getTime()) / 1000);
//
//        final int indexFrom = index(Arrays.binarySearch(
//            intervals.getMaxDepartures(), scheduledTimeFrom));
//        final int indexTo = index(Arrays.binarySearch(
//            intervals.getMinArrivals(), scheduledTimeTo));
//        for (int in = indexFrom; in < indexTo; in++) {
//          final BlockSequence bs = blockSequenceIdx.getSequences().get(in);
//          blockTripsForObs.addAll(bs.getBlockConfig().getTrips());
//          final BlockConfigurationEntry block = bs.getBlockConfig();
//          final BlockInstance instance = new BlockInstance(block,
//              serviceDate.getTime());
//          potentialBlocks.add(instance);
//        }
//      }
//    }
//
//    return blockTripsForObs;
//  }

  private int index(int index) {
    if (index < 0)
      return -(index + 1);
    return index;
  }

  @SuppressWarnings("unused")
  @Deprecated
  private void computeNearbyBlocksOld(Observation observation,
      Set<BlockInstance> potentialBlocks) {

    final NycRawLocationRecord record = observation.getRecord();
    final long time = observation.getTime();
    final long timeFrom = time - _tripSearchTimeAfterLastStop;
    final long timeTo = time + _tripSearchTimeBeforeFirstStop;

    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);

    final List<BlockLayoverIndex> layoverIndices = Collections.emptyList();
    final List<FrequencyBlockTripIndex> frequencyIndices = Collections.emptyList();

    final Set<BlockTripIndex> blockindices = new HashSet<BlockTripIndex>();

    final Set<AgencyAndId> dscRoutes = observation.getDscImpliedRouteCollections();
    final List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (final StopEntry stop : stops) {

      final List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService.getStopTimeIndicesForStop(stop);
      for (final BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        /*
         * when we can, restrict to stops that service the dsc-implied route
         */
        if (dscRoutes != null && !dscRoutes.isEmpty()) {
          for (final BlockTripEntry bentry : stopTimeIndex.getTrips()) {
            if (dscRoutes.contains(bentry.getTrip().getRouteCollection().getId())) {
              blockindices.add(_blockIndexFactoryService.createTripIndexForGroupOfBlockTrips(stopTimeIndex.getTrips()));
              break;
            }
          }
        } else {
          // TODO perhaps "fix" this use of blockIndexFactoryService.
          blockindices.add(_blockIndexFactoryService.createTripIndexForGroupOfBlockTrips(stopTimeIndex.getTrips()));
        }
      }
    }
    final List<BlockInstance> nearbyBlocks = _blockCalendarService.getActiveBlocksInTimeRange(
        blockindices, layoverIndices, frequencyIndices, timeFrom, timeTo);
    potentialBlocks.addAll(nearbyBlocks);
  }

}
