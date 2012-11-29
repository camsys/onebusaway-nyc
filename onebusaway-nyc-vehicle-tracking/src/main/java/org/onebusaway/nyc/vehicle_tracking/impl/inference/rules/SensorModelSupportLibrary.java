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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.collections.Min;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ScheduleDeviationLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel2;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;
import umontreal.iro.lecuyer.stat.Tally;

@Component
public class SensorModelSupportLibrary {

  /****
   * Services
   ****/

  private VehicleStateLibrary _vehicleStateLibrary;

  private DestinationSignCodeService _destinationSignCodeService;

  private BlockStateTransitionModel _blockStateTransitionModel;

  private ScheduleDeviationLibrary _scheduleDeviationLibrary;

  private BlockStateService _blockStateService;

  /****
   * Parameters
   ****/

  private final DeviationModel _travelToStartOfBlockRatioModel = new DeviationModel(
      0.5);

  private final DeviationModel _travelToStartOfBlockDistanceModel = new DeviationModel(
      500);

  /**
   * We penalize if you aren't going to start your block on time
   */
  private final static DeviationModel _startBlockOnTimeModel = new DeviationModel(
      20);

  /**
   * 30 mph = 48.28032 kph = 13.4112 m/sec
   */
  private final static double _averageSpeed = 13.4112;

  /**
   * In minutes
   */
  private final static int _maxLayover = 30;

  private final DeviationModel _blockLocationDeviationModel = new DeviationModel(
      50);

  private final boolean _useBlockLocationDeviationModel = true;

  private final DeviationModel _scheduleDeviationModel = new DeviationModel(
      50 * 60);

  private final double _propabilityOfBeingOutOfServiceWithAnOutOfServiceDSC = 0.95;

  private final double _propabilityOfBeingOutOfServiceWithAnInServiceDSC = 0.1;

  private final double _shortRangeProgressDistance = 500;

  /**
   * If we're more than X meters off our block path, then we really don't think
   * we're serving the block any more
   */
  private final double _offBlockDistance = 1000;

  private final static DeviationModel2 _endOfBlockDeviationModel = new DeviationModel2(
      200);

  /****
   * Service Setters
   ****/

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Autowired
  public void setBlocksStateService(BlockStateService blocksStateService) {
    _blockStateService = blocksStateService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBlockStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
    _blockStateTransitionModel = blockStateTransitionModel;
  }

  @Autowired
  public void setScheduleDeviationLibrary(
      ScheduleDeviationLibrary scheduleDeviationLibrary) {
    _scheduleDeviationLibrary = scheduleDeviationLibrary;
  }

  /****
   * Private Methods
   ****/

  public double computeAtBaseProbability(VehicleState state, Observation obs) {

    final boolean isAtBase = _vehicleStateLibrary.isAtBase(obs.getLocation());

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    /**
     * If we are in progress, then it's ok if we accidentally go by the base
     */
    if (EVehiclePhase.isActiveDuringBlock(phase))
      return 1.0;

    /**
     * RULE: AT_BASE <=> bus located at the base
     */
    return biconditional(p(phase == EVehiclePhase.AT_BASE), p(isAtBase));
  }

  /****
   * 
   ****/

  public double computeDestinationSignCodeProbability(VehicleState state,
      Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    final NycRawLocationRecord record = obs.getRecord();
    final String observedDsc = record.getDestinationSignCode();

    final boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc);

    /**
     * Rule: out-of-service DSC => ! IN_PROGRESS
     */

    final double p1 = implies(p(outOfService),
        p(phase != EVehiclePhase.IN_PROGRESS));

    return p1;
  }

  /****
   * 
   ****/

  public double computeBlockProbabilities(VehicleState state, Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    /**
     * Rule: vehicle in active phase => block assigned and not past the end of
     * the block
     */
    final boolean activeDuringBlock = EVehiclePhase.isActiveDuringBlock(phase);

    final double p1 = implies(p(activeDuringBlock), p(blockState != null
        && blockState.getBlockLocation().getNextStop() != null));

    /**
     * Rule: block assigned => on schedule
     */
    double pOnSchedule = 1.0;
    if (blockState != null
        && blockState.getBlockLocation().getNextStop() != null
        && activeDuringBlock)
      pOnSchedule = computeScheduleDeviationProbability(state, obs);

    return p1 * pOnSchedule;
  }

  /*****
   * 
   ****/

  public double computeDeadheadBeforeProbabilities(VehicleState parentState,
      VehicleState state, Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_BEFORE || blockState == null)
      return 1.0;

    /**
     * Rule: DEADHEAD_BEFORE => making progress towards start of the block
     */

    final double pLongTermProgressTowardsStartOfBlock = computeLongRangeProgressTowardsStartOfBlockProbability(
        state, obs);

    double pShortTermProgressTowardsStartOfBlock = 1.0;
    if (parentState != null)
      pShortTermProgressTowardsStartOfBlock = computeShortRangeProgressTowardsStartOfBlockProbability(
          parentState, state, obs);

    final double pProgressTowardsStartOfBlock = or(
        pLongTermProgressTowardsStartOfBlock,
        pShortTermProgressTowardsStartOfBlock);

    /**
     * Rule: DEADHEAD_BEFORE => start block on time
     */

    final double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, obs);

    return pProgressTowardsStartOfBlock * pStartBlockOnTime;
  }

  /**
   * @return the probability that the vehicle has not moved in a while
   */
  static public double computeVehicleHasNotMovedProbability(
      MotionState motionState, Observation obs) {

    final NycRawLocationRecord prevRecord = obs.getPreviousRecord();

    if (prevRecord == null)
      return 0.5;

    final double d = SphericalGeometryLibrary.distance(
        prevRecord.getLatitude(), prevRecord.getLongitude(),
        obs.getLocation().getLat(), obs.getLocation().getLon());

    /*
     * Although the gps std. dev is reasonable for having not moved, we also
     * have dead-reckoning, which is much more accurate in this regard, so we
     * shrink the gps std. dev.
     */
    final double prob = 1d - FoldedNormalDist.cdf(0d, GpsLikelihood.gpsStdDev, d);
    
    /*
     * We suspect some numerical issues here, so truncate...
     */
    if (prob < 0d)
      return 0d;
    else if (prob > 1d)
      return 1d;
    else
      return prob;
  }

  /*****
   * 
   ****/

  public double computeInProgressProbabilities(VehicleState parentState,
      VehicleState state, Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    /**
     * These probabilities only apply if are IN_PROGRESS and have a block state
     */
    if (phase != EVehiclePhase.IN_PROGRESS || blockState == null)
      return 1.0;

    /**
     * Rule: IN_PROGRESS => not making short term progress towards start of
     * block
     */
    double pNotMakingShortRangeProgressTowardsStartOfBlock = 1.0;
    if (parentState != null)
      pNotMakingShortRangeProgressTowardsStartOfBlock = 1.0 - computeShortRangeProgressTowardsStartOfBlockProbability(
          parentState, state, obs);

    /**
     * Rule: IN_PROGRESS => on route
     */
    final double pOnRoute = computeOnRouteProbability(state, obs);

    /**
     * Rule: IN_PROGRESS => block location is close to gps location
     */
    final double pBlockLocation = computeBlockLocationProbability(parentState,
        blockState, obs);

    return pNotMakingShortRangeProgressTowardsStartOfBlock * pOnRoute
        * pBlockLocation;
  }

  public double computeBlockLocationProbability(VehicleState parentState,
      BlockState blockState, Observation obs) {

    if (!_useBlockLocationDeviationModel)
      return 1.0;

    final Tally avg = new Tally();

    /**
     * The idea here is that we look for the absolute best block location given
     * our current observation, even if it means traveling backwards
     */
    for (final BlockStateObservation bso : _blockStateTransitionModel.getClosestBlockStates(
        blockState, obs)) {

      double prob = 0.0;
      final BlockState closestBlockState = bso.getBlockState();
      final ScheduledBlockLocation closestBlockLocation = closestBlockState.getBlockLocation();

      /**
       * We compare this against our best block location assuming a bus
       * generally travels forward
       */
      final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

      /**
       * If we're just coming out of a layover, there is some chance that the
       * block location was allowed to shift to the end of the layover to match
       * the underlying schedule and may be slightly ahead of our current block
       * location. We're ok with that.
       */
      if (parentState != null
          && EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())) {
        final double delta = blockLocation.getDistanceAlongBlock()
            - closestBlockLocation.getDistanceAlongBlock();

        if (0 <= delta && delta < 300)
          return 1.0;
      }

      /**
       * If the distance between the two points is high, that means that our
       * block location isn't great and might suggest we've been assigned a
       * block that is moving in the wrong direction
       */
      final double blockLocationDelta = SphericalGeometryLibrary.distance(
          closestBlockLocation.getLocation(), blockLocation.getLocation());
      prob = _blockLocationDeviationModel.probability(blockLocationDelta);
      avg.add(prob);
    }

    return avg.average();
  }

  public double computeScheduleDeviationProbability(VehicleState state,
      Observation observation) {

    final int delta = _scheduleDeviationLibrary.computeScheduleDeviation(state,
        observation);

    return _scheduleDeviationModel.probability(delta);
  }

  public double computeOnRouteProbability(VehicleState state, Observation obs) {

    final double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        obs, state.getBlockState());

    if (distanceToBlock <= _offBlockDistance)
      return 1.0;
    else
      return 0.0;
  }

  /****
   * 
   ****/

  public double computeDeadheadDuringProbabilities(VehicleState state,
      Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_DURING || blockState == null)
      return 1.0;

    /**
     * Rule: DEADHEAD_DURING <=> Vehicle has moved AND at layover location
     */

    final double pMoved = not(computeVehicleHasNotMovedProbability(
        state.getMotionState(), obs));

    final double pAtLayoverLocation = p(_vehicleStateLibrary.isAtPotentialLayoverSpot(
        state, obs));

    final double p1 = pMoved * pAtLayoverLocation;

    /**
     * Rule: DEADHEAD_DURING => not right on block
     */
    final double pDistanceFromBlock = computeDeadheadDistanceFromBlockProbability(
        obs, blockState);

    /**
     * Rule: DEADHEAD_DURING => resume block on time
     */
    final double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, obs);

    /**
     * Rule: DEADHEAD_DURING => served some part of block
     */
    final double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());

    return p1 * pDistanceFromBlock * pStartBlockOnTime * pServedSomePartOfBlock;
  }

  /****
   * 
   ****/

  public double computeDeadheadOrLayoverAfterProbabilities(VehicleState state,
      Observation obs) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();
    final BlockState blockState = state.getBlockState();

    /**
     * Why do we check for a block state here? We assume a vehicle can't
     * deadhead or layover after unless it's actively served a block
     */
    if (!EVehiclePhase.isActiveAfterBlock(phase) || blockState == null)
      return 1.0;

    /**
     * RULE: DEADHEAD_AFTER || LAYOVER_AFTER => reached the end of the block
     */

    final double pEndOfBlock = computeProbabilityOfEndOfBlock(state.getBlockState());

    /**
     * There is always some chance that the vehicle has served some part of the
     * block and then gone rogue: out-of-service or off-block. Unfortunately,
     * this is tricky because a vehicle could have been incorrectly assigned to
     * a block ever so briefly (fulfilling the served-some-part-of-the-block
     * requirement), it can quickly go off-block if the block assignment doesn't
     * keep up.
     */
    final double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
    final double pOffBlock = computeOffBlockProbability(state, obs);
    final double pOutOfService = computeOutOfServiceProbability(obs);

    final double offRouteOrOutOfService = pServedSomePartOfBlock
        * or(pOffBlock, pOutOfService);

    return or(pEndOfBlock, offRouteOrOutOfService);
  }

  /****
   * 
   ****/

  public double computePriorProbability(VehicleState state) {

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    /**
     * Technically, we might better weight these from prior training data, but
     * for now we want to slightly prefer being in service, not out of service
     */
    if (EVehiclePhase.isActiveDuringBlock(phase))
      return 1.0;
    else
      return 0.5;
  }

  /****
   * 
   ****/

  public double computeTransitionProbability(VehicleState parentState,
      VehicleState state, Observation obs) {

    if (parentState == null)
      return 1.0;

    final JourneyState parentJourneyState = parentState.getJourneyState();
    final JourneyState journeyState = state.getJourneyState();

    final EVehiclePhase parentPhase = parentJourneyState.getPhase();
    final EVehiclePhase phase = journeyState.getPhase();

    /**
     * Right now, we disallow this transition, but we will eventually re-enable
     * it under specific circumstances.
     */
    if (parentPhase == EVehiclePhase.DEADHEAD_AFTER
        && phase == EVehiclePhase.DEADHEAD_BEFORE)
      return 0.0;

    return 1.0;
  }

  /****
   * Various Probabilities
   ****/

  /**
   * Deadheading is only likely if we aren't super closer to the block path
   * itself
   */
  public double computeDeadheadDistanceFromBlockProbability(Observation obs,
      BlockState blockState) {

    // If we don't have a block, we could go either way
    if (blockState == null)
      return 0.5;

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    final CoordinatePoint location = blockLocation.getLocation();

    final double d = SphericalGeometryLibrary.distance(location,
        obs.getLocation());

    if (d > 100)
      return 1.0;

    return 0.25;
  }

  public double computeLongRangeProgressTowardsStartOfBlockProbability(
      VehicleState state, Observation obs) {

    final JourneyState js = state.getJourneyState();
    final BlockState blockState = state.getBlockState();

    final JourneyStartState startState = js.getData();
    final CoordinatePoint journeyStartLocation = startState.getJourneyStart();

    final CoordinatePoint currentLocation = obs.getLocation();

    // If we don't have a block assignment, are we really making progress?
    if (blockState == null)
      return 0.5;

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    final CoordinatePoint blockStart = blockLocation.getLocation();

    // Are we making progress towards the start of the block?
    final double directDistance = SphericalGeometryLibrary.distance(
        journeyStartLocation, blockStart);
    final double traveledDistance = SphericalGeometryLibrary.distance(
        currentLocation, journeyStartLocation);
    final double remainingDistance = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);

    /**
     * This method only applies if we're already reasonable close to the start
     * of the block
     */
    if (remainingDistance <= _shortRangeProgressDistance)
      return 0.0;

    if (directDistance < 500) {
      final double delta = remainingDistance - 500;
      return _travelToStartOfBlockDistanceModel.probability(delta);
    } else {
      // Ratio should be 1 if we drove directly from the depot to the start of
      // the
      // block but will increase as we go further out of our way
      final double ratio = (traveledDistance + remainingDistance)
          / directDistance;

      // This might not be the most mathematically appropriate model, but it's
      // close
      return _travelToStartOfBlockRatioModel.probability(ratio - 1.0);
    }
  }

  /**
   * The idea here is that if we're dead-heading to the beginning of a block, we
   * don't want to actually to start servicing the block until we've just
   * reached the beginning.
   * 
   * @param parentState
   * @param state
   * @param obs TODO
   * @return
   */
  public double computeShortRangeProgressTowardsStartOfBlockProbability(
      VehicleState parentState, VehicleState state, Observation obs) {

    final BlockState blockState = state.getBlockState();

    /**
     * If we don't have a block state, we can't be making progress towards the
     * start
     */
    if (blockState == null)
      return 0.0;

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    /**
     * If we don't have a parent state, we've just started and can be making
     * progress anywhere
     */
    if (parentState == null)
      return 1.0;

    /**
     * This method only applies if we haven't already started the block
     */
    final EVehiclePhase parentPhase = parentState.getJourneyState().getPhase();
    if (!EVehiclePhase.isActiveBeforeBlock(parentPhase))
      return 0.0;

    /**
     * This method only applies if we're still at the beginning of the block
     */
    if (blockLocation.getDistanceAlongBlock() > 200)
      return 0.0;

    final NycRawLocationRecord prev = obs.getPreviousRecord();
    final CoordinatePoint prevLocation = new CoordinatePoint(
        prev.getLatitude(), prev.getLongitude());

    final double dParent = SphericalGeometryLibrary.distance(prevLocation,
        blockLocation.getLocation());
    final double dNow = _vehicleStateLibrary.getDistanceToBlockLocation(obs,
        blockState);

    /**
     * This method only applies if we're already reasonable close to the start
     * of the block
     */
    if (dNow > _shortRangeProgressDistance)
      return 0.0;

    if (dNow <= dParent)
      return 1.0;

    return 0.0;
  }

  public double computeDeadheadDestinationSignCodeProbability(
      BlockState blockState, Observation observation) {

    final NycRawLocationRecord record = observation.getRecord();
    final String observedDsc = record.getDestinationSignCode();

    // If the driver hasn't set an in-service DSC yet, we can't punish too much
    if (_destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc))
      return 0.90;

    String blockDsc = null;
    if (blockState != null)
      blockDsc = blockState.getDestinationSignCode();

    // We do have an in-service DSC at this point, so we heavily favor blocks
    // that have the same DSC
    if (observedDsc.equals(blockDsc)) {
      return 0.95;
    } else {
      return 0.95;
    }
  }

  static public int startOrResumeBlockOnTimeDev(VehicleState state,
      Observation obs) {
    final CoordinatePoint currentLocation = obs.getLocation();

    final BlockState blockState = state.getBlockState();

    final BlockInstance blockInstance = blockState.getBlockInstance();
    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    final CoordinatePoint blockStart = blockLocation.getLocation();

    // in meters
    final double distanceToBlockStart = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);
    // in seconds
    final int timeToStart = (int) (distanceToBlockStart / _averageSpeed);

    final long estimatedStart = obs.getTime() + timeToStart * 1000;
    final long scheduledStart = blockInstance.getServiceDate()
        + blockLocation.getScheduledTime() * 1000;

    int minutesDiff = (int) ((scheduledStart - estimatedStart) / (1000 * 60));

    // Arriving early?
    if (minutesDiff >= 0) {
      // Allow for a layover
      minutesDiff = Math.max(0, minutesDiff - _maxLayover);
    }
    return Math.abs(minutesDiff);
  }

  static public double computeStartOrResumeBlockOnTimeProbability(
      VehicleState state, Observation obs) {

    final BlockState blockState = state.getBlockState();
    // If we don't have an assigned block yet, we hedge our bets on whether
    // we're starting on time or not
    if (blockState == null)
      return 0.5;

    final int minutesDiff = startOrResumeBlockOnTimeDev(state, obs);

    return _startBlockOnTimeModel.probability(minutesDiff);
  }

  public double computeOffBlockProbability(VehicleState state, Observation obs) {

    final double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        obs, state.getBlockState());

    if (_offBlockDistance < distanceToBlock)
      return 1.0;
    else
      return 0.0;
  }

  static public boolean hasServedSomePartOfBlock(BlockState blockState) {

    // If we don't have a block, then we haven't made progress on a block
    if (blockState == null)
      return false;

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    // How much of a block do we need to run to consider ourselves started?
    if (blockLocation.getDistanceAlongBlock() > 500)
      return true;
    else
      return false;
  }

  public double computeProbabilityOfServingSomePartOfBlock(BlockState blockState) {

    // If we don't have a block, then we haven't made progress on a block
    if (blockState == null)
      return 0.0;

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    // How much of a block do we need to run to consider ourselves started?
    if (blockLocation.getDistanceAlongBlock() > 500)
      return 1.0;
    else
      return 0.0;
  }

  public static double computeProbabilityOfEndOfBlock(BlockState blockState) {

    // If we don't have a block, we can't be at the end of a block
    if (blockState == null)
      return 0.0;

    final BlockInstance blockInstance = blockState.getBlockInstance();
    final BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    final double totalBlockDistance = blockConfig.getTotalBlockDistance();

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    final double distanceAlongBlock = blockLocation.getDistanceAlongBlock();

    final double delta = totalBlockDistance - distanceAlongBlock;
    return _endOfBlockDeviationModel.probability(delta);
  }

  public double computeOutOfServiceProbability(Observation observation) {

    final NycRawLocationRecord record = observation.getRecord();
    final String dsc = record.getDestinationSignCode();
    final boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);

    /**
     * Note that these two probabilities don't have to add up to 1, as they are
     * conditionally independent.
     */
    if (outOfService)
      return _propabilityOfBeingOutOfServiceWithAnOutOfServiceDSC;
    else
      return _propabilityOfBeingOutOfServiceWithAnInServiceDSC;
  }

  /****
   * 
   ****/

  /**
   * @return true if the the current DSC indicates the vehicle is out of service
   */
  public boolean isOutOfServiceDestinationSignCode(Observation obs) {

    final NycRawLocationRecord record = obs.getRecord();
    final String observedDsc = record.getDestinationSignCode();

    return _destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc);
  }

  /**
   * 
   * @return true if the distance between the observed bus location
   */
  public boolean isOnRoute(Context context) {

    final Observation obs = context.getObservation();
    final VehicleState state = context.getState();

    final double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        obs, state.getBlockState());

    return distanceToBlock <= _offBlockDistance;
  }

  /****
   * Core Probability Methods
   ****/

  public static double or(double... pValues) {
    if (pValues.length == 0)
      return 0.0;
    double p = pValues[0];
    for (int i = 1; i < pValues.length; i++)
      p = p + pValues[i] - (p * pValues[i]);
    return p;
  }

  public static double implies(double a, double b) {
    return or(1.0 - a, b);
  }

  public static double biconditional(double a, double b) {
    return implies(a, b) * implies(b, a);
  }

  public static double p(boolean b) {
    return b ? 1 : 0;
  }

  public static double p(boolean b, double pTrue) {
    return b ? pTrue : 1.0 - pTrue;
  }

  public static final double not(double p) {
    return 1.0 - p;
  }

  public BlockState getPreviousStateOnSameBlock(VehicleState state, double dabReference) {
    final Observation prevObs = state.getObservation().getPreviousObservation();
    try {
      final BestBlockStates prevState = _blockStateService.getBestBlockLocations(
          prevObs, state.getBlockState().getBlockInstance(),
          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      
      Min<BlockState> minstate = new Min<BlockState>();
      for (BlockState blockState : prevState.getAllStates()) {
        final double thisDabDiff = Math.abs(dabReference 
            - blockState.getBlockLocation().getDistanceAlongBlock());
        minstate.add(thisDabDiff, blockState);
      }
      return minstate.getMinElement();
    } catch (final MissingShapePointsException e) {
      e.printStackTrace();
    }
    return null;
  }

}
