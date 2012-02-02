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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.services.RouteService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexFactoryService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

  private VehicleStateLibrary _vehicleStateLibrary;

  private VehicleAssignmentService _vehicleAssignmentService;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  private BlockCalendarService _blockCalendarService;

  private RouteService _routeService;

  @Autowired
  public void setRouteService(RouteService routeService) {
    _routeService = routeService;
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

  private boolean _includeNearbyBlocks = true;

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
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
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

    EObservationCacheKey thisKey = bestBlockLocation
        ? EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK
        : EObservationCacheKey.JOURNEY_START_BLOCK;

    Set<BlockStateObservation> potentialBlocks = _observationCache.getValueForObservation(
        observation, thisKey);

    if (potentialBlocks == null) {
      potentialBlocks = Collections.newSetFromMap(new ConcurrentHashMap<BlockStateObservation, Boolean>());

      /*
       * we compute the nearby blocks NOW, for any methods that might use it as
       * the support for a distribution.
       */
      Set<BlockInstance> consideredBlocks = new HashSet<BlockInstance>();
      if (_includeNearbyBlocks)
        computeNearbyBlocks(observation, consideredBlocks);

      Set<BlockInstance> bisSet = determinePotentialBlocksForObservation(
          observation, consideredBlocks);

      consideredBlocks.addAll(bisSet);

      for (BlockInstance thisBIS : bisSet) {
        Set<BlockStateObservation> states = new HashSet<BlockStateObservation>();
        if (bestBlockLocation) {
          try {
            for (BlockState bs : _blockStateService.getBestBlockLocations(
                observation, thisBIS, 0, Double.POSITIVE_INFINITY).getAllStates()) {
              states.add(new BlockStateObservation(bs, observation));
            }
          } catch (MissingShapePointsException e) {
            _log.warn(e.getMessage());
            continue;
          }
        } else {
          try {
            states.add(new BlockStateObservation(_blockStateService.getAsState(
                thisBIS, 0.0), observation));
          } catch (Exception e) {
            _log.warn(e.getMessage());
            continue;
          }
        }
        potentialBlocks.addAll(states);
      }

      // /*
      // * NOTE: this will also set the run-state flags
      // */
      // potentialBlocks.addAll(getReportedBlockStates(observation,
      // consideredBlocks, bestBlockLocation, potentialBlocks));

      if (!potentialBlocks.isEmpty()) {
        if (_log.isDebugEnabled()) {
          for (BlockStateObservation reportedState : potentialBlocks) {
            _log.debug("vehicle=" + observation.getRecord().getVehicleId()
                + " reported block=" + reportedState + " for operator="
                + observation.getRecord().getOperatorId() + " run="
                + observation.getRecord().getRunNumber());
          }
        }

        /*
         * These reported/assigned runs -> blocks are now guaranteed to be
         * present in the particle set.
         */
      }

      _observationCache.putValueForObservation(observation, thisKey,
          potentialBlocks);
    }

    return potentialBlocks;

  }

  /****
   * {@link BlocksFromObservationService} Interface
   ****/

  @Override
  public Set<BlockInstance> determinePotentialBlocksForObservation(
      Observation observation, Set<BlockInstance> nearbyBlocks) {

    Set<BlockInstance> potentialBlocks = new HashSet<BlockInstance>();

    /**
     * Second source of trips: the destination sign code
     */
    if (observation.getLastValidDestinationSignCode() != null
        && !observation.isOutOfService()) {
      computePotentialBlocksFromDestinationSignCodeAndRunId(observation,
          potentialBlocks);
    }

    /**
     * Third source of trips: trips nearby the current gps location
     */
    if (_includeNearbyBlocks) {
      potentialBlocks.addAll(nearbyBlocks);
    }

    return potentialBlocks;
  }

  @Override
  public BestBlockObservationStates advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

    BestBlockObservationStates resStates = null;
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockState.getBlockInstance(), currentDistanceAlongBlock
              + minDistanceToTravel, currentDistanceAlongBlock
              + maxDistanceToTravel);
    } catch (MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }

    if (foundStates != null) {
      resStates = new BestBlockObservationStates(new BlockStateObservation(
          foundStates.getBestTime(), observation), new BlockStateObservation(
          foundStates.getBestLocation(), observation));
    }

    return resStates;
  }

  @Override
  public BlockStateObservation advanceLayoverState(Observation obs,
      BlockStateObservation blockState) {

    long timestamp = obs.getTime();
    BlockInstance instance = blockState.getBlockState().getBlockInstance();

    /**
     * The targetScheduleTime is the schedule time we SHOULD be at
     */
    int targetScheduleTime = (int) ((timestamp - instance.getServiceDate()) / 1000);

    ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();

    int scheduledTime = blockLocation.getScheduledTime();

    /**
     * First, we must actually be in a layover location
     */
    BlockStopTimeEntry layoverSpot = VehicleStateLibrary.getPotentialLayoverSpot(blockLocation);

    if (layoverSpot == null)
      return blockState;

    /**
     * The layover spot is the location between the last stop in the previous
     * trip and the first stop of the next trip
     */
    StopTimeEntry stopTime = layoverSpot.getStopTime();

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
      BlockConfigurationEntry blockConfig = instance.getBlock();
      int minArrivalTime = blockConfig.getArrivalTimeForIndex(layoverSpot.getBlockSequence() - 1);
      if (scheduledTime + 5 * 60 < minArrivalTime)
        return blockState;

      /**
       * We won't advance the vehicle out of the layover, so we put a lower
       * bound on our target schedule time
       */
      targetScheduleTime = Math.max(targetScheduleTime, minArrivalTime);
    }

    BlockStateObservation state = new BlockStateObservation(
        _blockStateService.getScheduledTimeAsState(instance, targetScheduleTime),
        obs);

    return state;
  }

  public static class BestBlockObservationStates {

    final BlockStateObservation _bestTime;
    final BlockStateObservation _bestLocation;

    public BestBlockObservationStates(BestBlockStates bestStates,
        Observation obs) {
      this._bestTime = new BlockStateObservation(bestStates.getBestTime(), obs);
      this._bestLocation = new BlockStateObservation(
          bestStates.getBestLocation(), obs);
    }

    public BestBlockObservationStates(BlockStateObservation bestTime,
        BlockStateObservation bestLocation) {
      if (bestTime == null || bestLocation == null)
        throw new IllegalArgumentException("best block states cannot be null");
      this._bestLocation = bestLocation;
      this._bestTime = bestTime;
    }

    public BlockStateObservation getBestTime() {
      return _bestTime;
    }

    public BlockStateObservation getBestLocation() {
      return _bestLocation;
    }

    public List<BlockStateObservation> getAllStates() {
      return _bestTime.equals(_bestLocation) ? Arrays.asList(_bestTime)
          : Arrays.asList(_bestTime, _bestLocation);
    }
  }

  /**
   * Finds the best block state assignments along the ENTIRE length of the block
   * (potentially expensive operation)
   */
  @Override
  public BestBlockObservationStates bestStates(Observation observation,
      BlockStateObservation blockState) {

    BlockInstance blockInstance = blockState.getBlockState().getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    BestBlockObservationStates resStates = null;
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockInstance, 0, Double.POSITIVE_INFINITY);
    } catch (MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }
    if (foundStates != null) {
      resStates = new BestBlockObservationStates(new BlockStateObservation(
          foundStates.getBestTime(), observation), new BlockStateObservation(
          foundStates.getBestLocation(), observation));
    }
    return resStates;
  }

  /*****
   * Private Methods
   ****/

  private void computePotentialBlocksFromDestinationSignCodeAndRunId(
      Observation observation, Set<BlockInstance> potentialBlocks) {

    long time = observation.getTime();

    /**
     * We use the last valid DSC, which will be the current DSC if it's not 0000
     * or the most recent good DSC otherwise
     */
    String dsc = observation.getLastValidDestinationSignCode();

    /**
     * Step 1: Figure out the set of all possible trip ids given the destination
     * sign code
     */
    List<AgencyAndId> dscTripIds = new ArrayList<AgencyAndId>();
    dscTripIds.addAll(_destinationSignCodeService.getTripIdsForDestinationSignCode(dsc));
    List<String> runIds = new ArrayList<String>();
    runIds.add(observation.getOpAssignedRunId());
    runIds.addAll(observation.getBestFuzzyRunIds());
    for (String runId : runIds) {
      dscTripIds.addAll(_runService.getTripIdsForRunId(runId));
    }

    if (dscTripIds.isEmpty()) {
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
    long timeFrom = time - _tripSearchTimeAfterLastStop;
    long timeTo = time + _tripSearchTimeBeforeFirstStop;

    for (AgencyAndId tripId : dscTripIds) {

      /**
       * Only consider a trip if it exists and has stop times
       */
      TripEntry trip = _transitGraphDao.getTripEntryForId(tripId);
      if (trip == null)
        continue;

      // TODO : Are we getting back more than we want as blocks will be active
      // longer than their corresponding trips?
      List<BlockInstance> instances = _activeCalendarService.getActiveBlocks(
          trip.getBlock().getId(), timeFrom, timeTo);
      potentialBlocks.addAll(instances);
    }

  }

  private void computeNearbyBlocks(Observation observation,
      Set<BlockInstance> potentialBlocks) {

    NycRawLocationRecord record = observation.getRecord();
    long time = observation.getTime();
    long timeFrom = time - _tripSearchTimeAfterLastStop;
    long timeTo = time + _tripSearchTimeBeforeFirstStop;

    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);

    List<BlockLayoverIndex> layoverIndices = Collections.emptyList();
    List<FrequencyBlockTripIndex> frequencyIndices = Collections.emptyList();

    Set<BlockTripIndex> blockindices = new HashSet<BlockTripIndex>();

    Set<AgencyAndId> dscRoutes = observation.getDscImpliedRouteCollections();
    List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (StopEntry stop : stops) {

      List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService.getStopTimeIndicesForStop(stop);
      for (BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        /*
         * when we can, restrict to stops that service the dsc-implied route
         */
        if (dscRoutes != null && !dscRoutes.isEmpty()) {
          for (BlockTripEntry bentry : stopTimeIndex.getTrips()) {
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
    List<BlockInstance> nearbyBlocks = _blockCalendarService.getActiveBlocksInTimeRange(
        blockindices, layoverIndices, frequencyIndices, timeFrom, timeTo);
    potentialBlocks.addAll(nearbyBlocks);
  }

}
