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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
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
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlocksFromObservationServiceImpl implements BlocksFromObservationService {

  private static Logger _log = LoggerFactory
      .getLogger(BlocksFromObservationServiceImpl.class);

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

  /**
   * 
   * @param tripSearchRadius
   *          in meters
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
  public Set<BlockState> determinePotentialBlockStatesForObservation(
      Observation observation, boolean bestBlockLocation) {

    Set<BlockState> potentialBlocks = new HashSet<BlockState>();

    /*
     * we compute the nearby blocks NOW, for any methods that might use it as
     * the support for a distribution.
     */
    Set<BlockInstance> consideredBlocks = new HashSet<BlockInstance>();
    computeNearbyBlocks(observation, consideredBlocks);
    
    Set<BlockInstance> bisSet = determinePotentialBlocksForObservation(
        observation, consideredBlocks);
    
    consideredBlocks.addAll(bisSet);
    
    for (BlockInstance thisBIS : bisSet) {
      Set<BlockState> states = new HashSet<BlockState>();
      if (bestBlockLocation) {
        states.addAll(_blockStateService.getBestBlockLocations(observation, thisBIS,
            0, Double.POSITIVE_INFINITY));
      } else {
        states.add(_blockStateService.getAsState(thisBIS, 0.0));
      }
      potentialBlocks.addAll(states);
    }
    
    /*
     * NOTE: this will also set the run-state flags
     */
    potentialBlocks.addAll(getReportedBlockStates(observation, consideredBlocks,
        bestBlockLocation, potentialBlocks));

    if (!potentialBlocks.isEmpty()) {
      if (_log.isDebugEnabled()) {
        for (BlockState reportedState : potentialBlocks) {
          _log.debug("vehicle=" + observation.getRecord().getVehicleId()
              + " reported block=" + reportedState + " for operator="
              + observation.getRecord().getOperatorId() + " run="
              + observation.getRecord().getRunNumber());
        }
      }

      /*
       * These reported/assigned runs -> blocks are now guaranteed to be present
       * in the particle set.
       */
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
      computePotentialBlocksFromDestinationSignCode(observation, potentialBlocks);
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
  public Set<BlockState> getReportedBlockStates(Observation observation,
      Set<BlockInstance> potentialBlocks, boolean bestBlockLocation, Set<BlockState> statesToUpdate) {

    Set<BlockState> blockStates = new HashSet<BlockState>();
    Date obsDate = observation.getRecord().getTimeAsDate();
    String opAssignedRunId = null;
    String operatorId = observation.getRecord().getOperatorId();

    if (StringUtils.isEmpty(operatorId)) {
      _log.warn("no operator id reported");
      // FIXME TODO use new model stuff
    } else {
      try {
        OperatorAssignmentItem oai = _operatorAssignmentService
            .getOperatorAssignmentItemForServiceDate(obsDate, operatorId);
        if (oai != null) {
          opAssignedRunId = oai.getRunId();
        }
      } catch (Exception e) {
        // what do do if the OAS is temporarily unavailable? retry again?
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
        _log.warn("couldn't find operator's assigned runTripEntry for runId="
            + opAssignedRunId + ", time=" + obsDate.getTime() + ", agency="
            + agencyId);
      } else {
        runEntriesToTry.add(opAssignedRunTrip);
      }
    } else {
      _log.warn("no assigned run found for operator=" + operatorId);
    }

    List<RunTripEntry> reportedRtes = new ArrayList<RunTripEntry>();
    boolean assignedReportedRunMismatch = false;

    if (StringUtils.isNotEmpty(reportedRunId)) {

      // TODO should we really match 000-000?
      reportedRunId = observation.getRecord().getRunId();

      /*
       * FIXME without "weighing", searching by nearby blocks is, in conjunction
       * with nearby block searches alone, not completely useful other than
       * perhaps to confirm that the assigned and reported runId match, and/or
       * narrow down the blocks to consider...
       */
      try {
        TreeMap<Integer, List<RunTripEntry>> fuzzyReportedMatches = _runService
            .getRunTripEntriesForFuzzyIdAndTime(new AgencyAndId(agencyId,
                reportedRunId), potentialBlocks, obsDate.getTime());
        if (fuzzyReportedMatches.isEmpty()) {
          _log.warn("couldn't find a fuzzy match for reported runId="
              + reportedRunId);
        } else {
          /*
           * FIXME this is a bit of a hack, but it helps for now. essentially,
           * we only deal with the best matches. makes sense, but only if there
           * is a good deal of consistency among the id's involved, and a fair
           * ranking by using the Levenshtein distance.
           */
          int bestDist = fuzzyReportedMatches.firstKey();

          /*
           * if there is no assigned runTrip, then use the fuzzy matches.
           * otherwise, check that the fuzzy matches contain the assigned
           * run-id, and, if so, use just that (implemented by not adding to the
           * runEntriesToTry).
           */
          if (opAssignedRunTrip != null) {
            if (!fuzzyReportedMatches.get(bestDist).contains(opAssignedRunTrip)) {
              _log.warn("operator assigned runTrip=" + opAssignedRunId
                  + " not found among best reported-run matches");
              assignedReportedRunMismatch = true;
            }
          } else {
            // FIXME don't keep this reportedRtes set around just for matching
            // later
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
        boolean opAssigned = StringUtils
            .equals(opAssignedRunId, rte.getRunId());
        boolean runReported = (reportedRtes != null && reportedRtes
            .contains(rte));

        BlockEntry blockEntry = rte.getTripEntry().getBlock();

        BlockInstance blockInstance = _runService
            .getBlockInstanceForRunTripEntry(rte, obsDate);

        if (blockInstance == null) {
          _log.info("couldn't find block instance for blockEntry=" + blockEntry
              + ", time=" + obsDate.getTime());
          continue;
        } else {

          Set<BlockState> states = null;
          if (bestBlockLocation) {
            states = _blockStateService.getBestBlockLocations(observation,
                blockInstance, 0, Double.POSITIVE_INFINITY);
          } else {
            states = new HashSet<BlockState>();
            states.add(_blockStateService.getAsState(blockInstance, 0.0));
          }

          for (BlockState state : states) {
            state.setRunReported(runReported);
            state.setOpAssigned(opAssigned);
            state.setRunReportedAssignedMismatch(assignedReportedRunMismatch);
            blockStates.add(state);
          }
        }
      }
    } else {
      _log.info("no operator id or run reported");
    }
    
    /*
     * now, set the run-status for the pre-computed block-states
     */
    for (BlockState blockState : statesToUpdate) {
      RunTripEntry rte = blockState.getRunTripEntry();
      
      boolean opAssigned = StringUtils
          .equals(opAssignedRunId, rte.getRunId());
      boolean runReported = (reportedRtes != null && reportedRtes
          .contains(rte));

      blockState.setRunReported(runReported);
      blockState.setOpAssigned(opAssigned);
      blockState.setRunReportedAssignedMismatch(assignedReportedRunMismatch);
      
    }

    return blockStates;
  }

  @Override
  public Set<BlockState> advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();
    
    Set<BlockState> resStates = _blockStateService.getBestBlockLocations(observation,
        blockState.getBlockInstance(), currentDistanceAlongBlock
            + minDistanceToTravel, currentDistanceAlongBlock
            + maxDistanceToTravel);
    
    /*
     * keep these flags alive
     * TODO must be a better/more consistent way
     */
    if (!resStates.isEmpty() && blockState.getOpAssigned() != null) {
      for (BlockState state : resStates) {
        state.setOpAssigned(blockState.getOpAssigned());
        state.setRunReported(blockState.getRunReported());
        state.setRunReportedAssignedMismatch(blockState.isRunReportedAssignedMismatch());
      }
    }

    return resStates;
  }

  @Override
  public BlockState advanceLayoverState(long timestamp, BlockState blockState) {

    BlockInstance instance = blockState.getBlockInstance();

    /**
     * The targetScheduleTime is the schedule time we SHOULD be at
     */
    int targetScheduleTime = (int) ((timestamp - instance.getServiceDate()) / 1000);

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    int scheduledTime = blockLocation.getScheduledTime();

    /**
     * First, we must actually be in a layover location
     */
    BlockStopTimeEntry layoverSpot = VehicleStateLibrary
        .getPotentialLayoverSpot(blockLocation);

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
      int minArrivalTime = blockConfig.getArrivalTimeForIndex(layoverSpot
          .getBlockSequence() - 1);
      if (scheduledTime + 5 * 60 < minArrivalTime)
        return blockState;

      /**
       * We won't advance the vehicle out of the layover, so we put a lower
       * bound on our target schedule time
       */
      targetScheduleTime = Math.max(targetScheduleTime, minArrivalTime);
    }

    BlockState state = _blockStateService.getScheduledTimeAsState(instance,
        targetScheduleTime);
    if (state != null) {
      state.setOpAssigned(blockState.getOpAssigned());
      state.setRunReported(blockState.getRunReported());
      state.setRunReportedAssignedMismatch(blockState.isRunReportedAssignedMismatch());
    }
    return state;
  }

  /**
   * Finds the best block state assignments along the ENTIRE length of the block
   * (potentially expensive operation)
   */
  public Set<BlockState> bestStates(Observation observation, BlockState blockState) {

    BlockInstance blockInstance = blockState.getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    Set<BlockState> resStates = _blockStateService.getBestBlockLocations(observation, blockInstance,
        0, blockConfig.getTotalBlockDistance());
    /*
     * keep these flags alive
     * TODO must be a better/more consistent way
     */
    if (!resStates.isEmpty()) {
      for (BlockState state : resStates) {
        state.setOpAssigned(blockState.getOpAssigned());
        state.setRunReported(blockState.getRunReported());
        state.setRunReportedAssignedMismatch(blockState.isRunReportedAssignedMismatch());
      }
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
    List<AgencyAndId> dscTripIds = _destinationSignCodeService
        .getTripIdsForDestinationSignCode(dsc);

    if (dscTripIds == null) {
      _log.warn("no trips found for dsc: " + dsc);
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

    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);

    List<BlockLayoverIndex> layoverIndices = Collections.emptyList();
    List<FrequencyBlockTripIndex> frequencyIndices = Collections.emptyList();

    Set<BlockTripIndex> blockindices = new HashSet<BlockTripIndex>();

    List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (StopEntry stop : stops) {
      List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService
          .getStopTimeIndicesForStop(stop);
      for (BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        // TODO perhaps "fix" this use of blockIndexFactoryService.
        blockindices.add(_blockIndexFactoryService
            .createTripIndexForGroupOfBlockTrips(stopTimeIndex.getTrips()));
      }
    }
    List<BlockInstance> nearbyBlocks = _blockCalendarService
        .getActiveBlocksInTimeRange(blockindices, layoverIndices,
            frequencyIndices, time, time);
    potentialBlocks.addAll(nearbyBlocks);
  }
}
