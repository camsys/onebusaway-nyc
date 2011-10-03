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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
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

  private BlockGeospatialService _blockGeospatialService;

  private BlockStateService _blockStateService;

  private VehicleStateLibrary _vehicleStateLibrary;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  private BlockCalendarService _blockCalendarService;

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

  private boolean _includeNearbyBlocks = false;

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
  public void setBlockGeospatialService(
      BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
  }

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
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

    potentialBlocks.addAll(getReportedBlockStates(observation,
        bestBlockLocation));

    /**
     * First source of trips: UTS/op determined run, reported run
     */
    if (!potentialBlocks.isEmpty()) {

      // FIXME should be debug
      // if (_log.isDebugEnabled())
      for (BlockState reportedState : potentialBlocks)
        _log.info("reported block=" + reportedState + " for operator="
            + observation.getRecord().getOperatorId() + " run="
            + observation.getRecord().getRunId());

      // TODO when should we just make sure we have the reported run/block in
      // the support, // compared to making it the only block. when there is
      // no UTS match yet a valid reported run/block?
      return potentialBlocks;
    }

    Set<BlockInstance> bisSet = determinePotentialBlocksForObservation(observation);
    for (BlockInstance thisBIS : bisSet) {
      BlockState state = null;
      if (bestBlockLocation) {
        state = _blockStateService.getBestBlockLocation(observation, thisBIS,
            0, Double.POSITIVE_INFINITY);
      } else {
        state = _blockStateService.getAsState(thisBIS, 0.0);
      }
      potentialBlocks.add(state);
    }

    return potentialBlocks;

  }

  /****
   * {@link BlocksFromObservationService} Interface
   ****/

  @Override
  public Set<BlockInstance> determinePotentialBlocksForObservation(
      Observation observation) {

    Set<BlockInstance> potentialBlocks = new HashSet<BlockInstance>();

    /**
     * Second source of trips: the destination sign code
     */
    computePotentialBlocksFromDestinationSignCode(observation, potentialBlocks);

    /**
     * Third source of trips: trips nearby the current gps location Ok we're not
     * doing this for now
     */
    if (_includeNearbyBlocks)
      computeNearbyBlocks(observation, potentialBlocks);

    return potentialBlocks;
  }

  /****
   * {@link BlocksFromObservationService} Interface
   ****/
  @Override
  public Set<BlockState> getReportedBlockStates(Observation observation,
      boolean bestBlockLocation) {

    Set<BlockState> blockStates = new HashSet<BlockState>();
    Date obsDate = observation.getRecord().getTimeAsDate();
    String utsRunId = null;
    String operatorId = observation.getRecord().getOperatorId();
    Date serviceDate = null;

    if (operatorId == null) {

      _log.warn("no operator id reported");
      // FIXME TODO use new model stuff

    } else {
      OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItem(obsDate, operatorId);
      if (oai != null) {
        utsRunId = oai.getRunId();
        serviceDate = oai.getServiceDate();
      }
    }

    String reportedRunId = observation.getRecord().getRunId();

    if (utsRunId != null || reportedRunId != null) {

      String runId = utsRunId != null ? utsRunId : reportedRunId;

      // TODO change to ReportedRunState enum
      boolean utsReported = true;
      boolean runReported = true;
      boolean utsReportedRunMismatch = false;

      if (utsRunId == null) {
        _log.warn("no UTS assigned run found for operator=" + operatorId);
        runId = reportedRunId;
        utsReported = false;

      } else if (!utsRunId.equals(reportedRunId)) {
        _log.warn("UTS assigned run (" + utsRunId + " and reported run ("
            + reportedRunId + " don't match.  defaulting to UTS run.");

        // TODO which to use? for now, uts...
        utsReportedRunMismatch = true;
      } else {
        runReported = false;
      }

      String agencyId = observation.getRecord().getVehicleId().getAgencyId();
      RunTripEntry rte = _runService.getRunTripEntryForRunAndTime(
          new AgencyAndId(agencyId, runId), obsDate.getTime());

      if (rte == null) {
        _log.error("couldn't find runTripEntry for runId=" + runId + ", time="
            + obsDate.getTime() + ", agency=" + agencyId);
        return blockStates;
      }

      List<BlockInstance> blockInstances = new ArrayList<BlockInstance>();
      BlockEntry blockEntry = rte.getTripEntry().getBlock();

      if (serviceDate == null) {
        blockInstances.addAll(_runService.getBlockInstancesForRunTripEntry(rte,
            obsDate.getTime()));
      } else {

        // TODO turn this into a helper method?
        Calendar cal = Calendar.getInstance();
        cal.setTime(serviceDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long serviceDateMs = cal.getTimeInMillis();

        BlockInstance bInst = _blockCalendarService.getBlockInstance(
            blockEntry.getId(), serviceDateMs);

        if (bInst == null) {
          _log.error("couldn't find blockInstance for UTS serviceDate ="
              + serviceDate);
          blockInstances.addAll(_runService.getBlockInstancesForRunTripEntry(
              rte, obsDate.getTime()));
        } else {
          blockInstances.add(bInst);
        }
      }

      if (blockInstances.isEmpty()) {
        _log.error("couldn't find block instances for blockEntry=" + blockEntry
            + ", serviceDate=" + serviceDate);
        return blockStates;
      } else {

        for (BlockInstance blockInst : blockInstances) {
          BlockState state = null;
          if (bestBlockLocation) {
            state = _blockStateService.getBestBlockLocation(observation,
                blockInst, rte, 0, Double.POSITIVE_INFINITY);
          } else {
            state = _blockStateService.getAsState(blockInst, rte, 0.0);
          }

          state.setRunReported(runReported);
          state.setUTSassigned(utsReported);
          state.setRunReportedUTSMismatch(utsReportedRunMismatch);
          blockStates.add(state);
        }
        return blockStates;
      }

    } else {

      _log.warn("no operator id or run reported");

      return blockStates;
    }
  }

  @Override
  public BlockState advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

    return _blockStateService.getBestBlockLocation(observation,
        blockState.getBlockInstance(), currentDistanceAlongBlock
            + minDistanceToTravel, currentDistanceAlongBlock
            + maxDistanceToTravel);
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
    BlockStopTimeEntry layoverSpot = _vehicleStateLibrary
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

    return _blockStateService.getScheduledTimeAsState(instance,
        targetScheduleTime);
  }

  /**
   * Finds the best block state assignment along the ENTIRE length of the block
   * (potentially expensive operation)
   */
  public BlockState bestState(Observation observation, BlockState blockState) {

    BlockInstance blockInstance = blockState.getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    return _blockStateService.getBestBlockLocation(observation, blockInstance,
        0, blockConfig.getTotalBlockDistance());
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

    List<BlockInstance> blocks = _blockGeospatialService
        .getActiveScheduledBlocksPassingThroughBounds(bounds, time, time);
    potentialBlocks.addAll(blocks);
  }
}
