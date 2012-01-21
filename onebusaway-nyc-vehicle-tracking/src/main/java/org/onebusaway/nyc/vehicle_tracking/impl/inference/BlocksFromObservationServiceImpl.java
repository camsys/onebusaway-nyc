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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
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
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
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
public class BlocksFromObservationServiceImpl implements BlocksFromObservationService {

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
  private long _tripSearchTimeAfteLastStop = 30 * 60 * 1000;

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
    _tripSearchTimeAfteLastStop = tripSearchTimeAfteLastStop;
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
            for(BlockState bs : _blockStateService.getBestBlockLocations(observation,
                  thisBIS, 0, Double.POSITIVE_INFINITY).getAllStates()) {
              states.add(new BlockStateObservation(bs));
            }
          } catch (MissingShapePointsException e) {
            _log.warn(e.getMessage());
            continue;
          }
        } else {
          try {
            states.add(new BlockStateObservation(_blockStateService.getAsState(thisBIS, 0.0)));
          } catch (Exception e) {
            _log.warn(e.getMessage());
            continue;
          }
        }
        potentialBlocks.addAll(states);
      }

      /*
       * NOTE: this will also set the run-state flags
       */
      potentialBlocks.addAll(getReportedBlockStates(observation,
          consideredBlocks, bestBlockLocation, potentialBlocks));

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
      computePotentialBlocksFromDestinationSignCode(observation,
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

  /****
   * {@link BlocksFromObservationService} Interface
   ****/
  @Override
  public Set<BlockStateObservation> getReportedBlockStates(
      Observation observation, Set<BlockInstance> potentialBlocks,
      boolean bestBlockLocation, Set<BlockStateObservation> statesToUpdate) {

    Set<BlockStateObservation> blockStates = new HashSet<BlockStateObservation>();
    Date obsDate = new Date(observation.getTime());

    String opAssignedRunId = null;
    String operatorId = observation.getRecord().getOperatorId();

    boolean noOperatorIdGiven = StringUtils.isEmpty(operatorId)
        || StringUtils.containsOnly(operatorId, "0");

    if (!noOperatorIdGiven) {
      try {
        OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
            new ServiceDate(obsDate), operatorId);

        if (oai != null) {
          opAssignedRunId = oai.getRunId();
        }
      } catch (Exception e) {
        _log.warn(e.getMessage());
      }
    }

    Set<RunTripEntry> runEntriesToTry = new HashSet<RunTripEntry>();

    String reportedRunId = observation.getRecord().getRunId();
    String agencyId = observation.getRecord().getVehicleId().getAgencyId();

    RunTripEntry opAssignedRunTrip = null;
    if (StringUtils.isNotEmpty(opAssignedRunId)) {
      opAssignedRunTrip = _runService.getActiveRunTripEntryForRunAndTime(
          new AgencyAndId(agencyId, opAssignedRunId), obsDate.getTime());
      if (opAssignedRunTrip == null) {
        _log.info("couldn't find operator's assigned runTripEntry for runId="
            + opAssignedRunId + ", time=" + obsDate.getTime() + ", agency="
            + agencyId);
      } else {
        runEntriesToTry.add(opAssignedRunTrip);
      }
    } else if (StringUtils.isEmpty(opAssignedRunId) && !noOperatorIdGiven) {
      _log.info("no assigned run found for operator=" + operatorId);
    }

    List<RunTripEntry> reportedRtes = new ArrayList<RunTripEntry>();
    boolean assignedReportedRunMismatch = false;

    if (StringUtils.isNotEmpty(reportedRunId)
        && !StringUtils.containsOnly(reportedRunId, new char[] {'0', '-'})) {

      try {
        TreeMap<Integer, List<RunTripEntry>> fuzzyReportedMatches = _runService.getRunTripEntriesForFuzzyIdAndTime(
            new AgencyAndId(agencyId, reportedRunId), potentialBlocks,
            obsDate.getTime());
        if (fuzzyReportedMatches.isEmpty()) {
          _log.info("couldn't find a fuzzy match for reported runId="
              + reportedRunId);
        } else {

          int bestDist = fuzzyReportedMatches.firstKey();

          /*
           * if there is no assigned runTrip, then use the fuzzy matches.
           * otherwise, check that the fuzzy matches contain the assigned
           * run-id, and, if so, use just that (implemented by not adding to the
           * runEntriesToTry).
           * 
           * FIXME We make a cutoff at levenshtein distance of 2, since much
           * more of a distance could easily be a totally different, but valid,
           * run number and route.
           */
          if (opAssignedRunTrip != null) {
            if (!fuzzyReportedMatches.get(bestDist).contains(opAssignedRunTrip)) {
              _log.info("operator assigned runTrip=" + opAssignedRunId
                  + " not found among best reported-run matches");
              assignedReportedRunMismatch = true;
            }
          } else if (bestDist <= 2) {
            // TODO don't keep reportedRtes around just for matching
            observation.setFuzzyMatchDistance(bestDist);
            reportedRtes.addAll(fuzzyReportedMatches.get(bestDist));
            runEntriesToTry.addAll(reportedRtes);
          }
        }
      } catch (IllegalArgumentException ex) {
        _log.warn(ex.getMessage());
      }

    }

    if (!runEntriesToTry.isEmpty()) {
      for (RunTripEntry rte : runEntriesToTry) {

        // TODO change to ReportedRunState enum
        boolean opAssigned = StringUtils.equals(opAssignedRunId, rte.getRunId());
        boolean runReported = (reportedRtes != null && reportedRtes.contains(rte));

        BlockEntry blockEntry = rte.getTripEntry().getBlock();

        BlockInstance blockInstance = _runService.getBlockInstanceForRunTripEntry(
            rte, obsDate);

        if (blockInstance == null) {
          _log.info("couldn't find block instance for blockEntry=" + blockEntry
              + ", time=" + obsDate.getTime());
          continue;
        } else {

          List<BlockState> states = null;
          if (bestBlockLocation) {
            try {
              states = _blockStateService.getBestBlockLocations(observation,
                  blockInstance, 0, Double.POSITIVE_INFINITY).getAllStates();
            } catch (MissingShapePointsException e) {
              _log.warn(e.getMessage());
              continue;
            }
          } else {
            // states = new HashSet<BlockState>();
            // states.add(_blockStateService.getAsState(blockInstance, 0.0));
            states = Arrays.asList(_blockStateService.getAsState(blockInstance,
                0.0));
          }

          for (BlockState state : states) {
            blockStates.add(new BlockStateObservation(state, runReported,
                opAssigned, assignedReportedRunMismatch));
          }
        }
      }
    } else {
      _log.info("no operator id or run reported for vehicle="
          + observation.getRecord().getVehicleId());
    }

    /*
     * now, set the run-status for the pre-computed block-states
     */
    for (BlockStateObservation blockState : statesToUpdate) {
      RunTripEntry rte = blockState.getBlockState().getRunTripEntry();

      /**
       * TODO this might need to be changed/updated to account for changes in
       * operator-id or reported-run when the filter isn't reset.
       */
      boolean opAssigned = StringUtils.equals(opAssignedRunId, rte.getRunId());
      Observation prevObs = observation.getPreviousObservation();

      if (blockState.getRunReported() != null
          && prevObs != null
          && observation.getFuzzyMatchDistance() <= prevObs.getFuzzyMatchDistance())
        blockState.setRunReported(blockState.getRunReported());
      else
        blockState.setRunReported(reportedRtes != null
            && reportedRtes.contains(rte));

      blockState.setOpAssigned(opAssigned);
      blockState.setRunReportedAssignedMismatch(assignedReportedRunMismatch);
    }

    return blockStates;
  }

  @Override
  public BestBlockObservationStates advanceState(Observation observation,
      BlockStateObservation blockState, double minDistanceToTravel,
      double maxDistanceToTravel) {

    ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();
    double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

    BestBlockObservationStates resStates = null;
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockState.getBlockState().getBlockInstance(), currentDistanceAlongBlock
              + minDistanceToTravel, currentDistanceAlongBlock
              + maxDistanceToTravel);
    } catch (MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }

    if (foundStates != null) {
      resStates = new BestBlockObservationStates(new BlockStateObservation(foundStates.getBestTime()),
          new BlockStateObservation(foundStates.getBestLocation()));
      resStates.getBestTime().transitionRunIdResults(observation, blockState);
      resStates.getBestLocation().transitionRunIdResults(observation, blockState);
    }

    return resStates;
  }

  @Override
  public BlockStateObservation advanceLayoverState(Observation obs, BlockStateObservation blockState) {

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

    BlockStateObservation state = new BlockStateObservation(_blockStateService.getScheduledTimeAsState(instance,
        targetScheduleTime));
    if (state != null) {
      state.transitionRunIdResults(obs, blockState);
    }
    return state;
  }

  public static class BestBlockObservationStates {

    final BlockStateObservation _bestTime;
    final BlockStateObservation _bestLocation;
    
    public BestBlockObservationStates(BestBlockStates bestStates) {
      this._bestTime = new BlockStateObservation(bestStates.getBestTime());
      this._bestLocation = new BlockStateObservation(bestStates.getBestLocation());
    }
    
    public BestBlockObservationStates(
        BlockStateObservation bestTime,
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
  public BestBlockObservationStates bestStates(Observation observation,
      BlockStateObservation blockState) {

    BlockInstance blockInstance = blockState.getBlockState().getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    BestBlockObservationStates resStates = null;
    BestBlockStates foundStates = null;
    try {
      foundStates = _blockStateService.getBestBlockLocations(observation,
          blockInstance, 0, blockConfig.getTotalBlockDistance());
    } catch (MissingShapePointsException e) {
      _log.warn(e.getMessage());
    }
    if (foundStates != null) {
      resStates = new BestBlockObservationStates(new BlockStateObservation(foundStates.getBestTime()),
          new BlockStateObservation(foundStates.getBestLocation()));
      resStates.getBestTime().transitionRunIdResults(observation, blockState);
      resStates.getBestLocation().transitionRunIdResults(observation, blockState);
    }
    return resStates;
  }

  /*****
   * Private Methods
   ****/


  private void computePotentialBlocksFromDestinationSignCode(
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
    List<AgencyAndId> dscTripIds = _destinationSignCodeService.getTripIdsForDestinationSignCode(dsc);

    if (dscTripIds == null) {
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
    long timeFrom = time - _tripSearchTimeAfteLastStop;
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
    long timeFrom = time - _tripSearchTimeAfteLastStop;
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
