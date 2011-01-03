package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel2;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SensorModelImpl implements SensorModel<Observation> {

  /****
   * Services
   ****/

  private VehicleStateLibrary _vehicleStateLibrary;

  private DestinationSignCodeService _destinationSignCodeService;

  private BlockStateTransitionModel _blockStateTransitionModel;

  private ScheduleDeviationLibrary _scheduleDeviationLibrary;

  /****
   * Parameters
   ****/

  private DeviationModel _travelToStartOfBlockRatioModel = new DeviationModel(
      0.5);

  private DeviationModel _travelToStartOfBlockDistanceModel = new DeviationModel(
      500);

  /**
   * We penalize if you aren't going to start your block on time
   */
  private DeviationModel _startBlockOnTimeModel = new DeviationModel(20);

  /**
   * 30 mph = 48.28032 kph = 13.4112 m/sec
   */
  private double _averageSpeed = 13.4112;

  /**
   * In minutes
   */
  private int _maxLayover = 30;

  /**
   * How long in minutes do we let a vehicle run past its next stop arrival
   * time?
   */
  private DeviationModel _vehicleIsOnScheduleModel = new DeviationModel(5);

  private DeviationModel _blockLocationDeviationModel = new DeviationModel(50);

  private boolean _useBlockLocationDeviationModel = true;

  private DeviationModel _scheduleDeviationModel = new DeviationModel(15 * 60);

  /**
   * What are the odds of switching blocks mid-stream? LOW!
   */
  private double _probabilityOfBlockChangeWhileInProgress = 0.01;

  private double _propabilityOfBeingOutOfServiceWithAnOutOfServiceDSC = 0.95;

  private double _propabilityOfBeingOutOfServiceWithAnInServiceDSC = 0.01;

  /**
   * We shouldn't be in unknown state if we have an out-of-service DSC
   */
  private double _propabilityOfUnknownWithAnOutOfServiceDSC = 0.05;

  private double _shortRangeProgressDistance = 500;

  /**
   * If we're more than X meters off our block path, then we really don't think
   * we're serving the block any more
   */
  private double _offBlockDistance = 1000;

  private DeviationModel2 _endOfBlockDeviationModel = new DeviationModel2(200);

  /****
   * Service Setters
   ****/

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
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
   * {@link SensorModel} Interface
   ****/

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();
    VehicleState parentState = null;
    Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    double pJourney = likelihood(parentState, state, observation);

    return pJourney;
  }

  /****
   * {@link SensorModelImpl} Interface
   ****/

  public double likelihood(VehicleState parentState, VehicleState state,
      Observation obs) {

    double pAtBase = computeAtBaseProbability(state, obs);

    double pDestinationSignCode = computeDestinationSignCodeProbability(state,
        obs);

    double pBlock = computeBlockProbabilities(state, obs);

    double pBlockChange = computeBlockSwitchProbability(parentState, state, obs);

    double pLayover = computeLayoverProbabilities(state, obs);

    double pDeadheadBefore = computeDeadheadBeforeProbabilities(parentState,
        state, obs);

    double pInProgress = computeInProgressProbabilities(parentState, state, obs);

    double pDeadheadDuring = computeDeadheadDuringProbabilities(state, obs);

    double pDeadheadAfter = computeDeadheadOrLayoverAfterProbabilities(state,
        obs);

    double pPrior = computePriorProbability(state);

    double pTransition = computeTransitionProbability(parentState, state, obs);

    return pAtBase * pDestinationSignCode * pBlock * pBlockChange * pLayover
        * pDeadheadBefore * pInProgress * pDeadheadDuring * pDeadheadAfter
        * pPrior * pTransition;
  }

  /****
   * Private Methods
   ****/

  public double computeAtBaseProbability(VehicleState state, Observation obs) {

    boolean isAtBase = _vehicleStateLibrary.isAtBase(obs.getLocation());

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

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

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc);

    /**
     * Rule: out-of-service DSC => ! IN_PROGRESS
     */

    double p1 = implies(p(outOfService), p(phase != EVehiclePhase.IN_PROGRESS));

    return p1;
  }

  /****
   * 
   ****/

  public double computeBlockProbabilities(VehicleState state, Observation obs) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    /**
     * Rule: vehicle in active phase => block assigned and not past the end of
     * the block
     */
    boolean activeDuringBlock = EVehiclePhase.isActiveDuringBlock(phase);

    double p1 = implies(p(activeDuringBlock), p(blockState != null
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

  /****
   * 
   ****/

  public double computeBlockSwitchProbability(VehicleState parentState,
      VehicleState state, Observation obs) {

    if (parentState == null)
      return 1.0;

    BlockState parentBlockState = parentState.getBlockState();
    BlockState blockState = state.getBlockState();

    if (parentBlockState == null)
      return 1.0;

    if (blockState == null)
      return 1.0;

    if (parentBlockState.getBlockInstance().equals(
        blockState.getBlockInstance()))
      return 1.0;

    JourneyState journeyState = state.getJourneyState();
    EVehiclePhase phase = journeyState.getPhase();

    /**
     * The probability of switching a block while actively in progress on
     * another block should be pretty low.
     */
    if (EVehiclePhase.isActiveDuringBlock(phase))
      return _probabilityOfBlockChangeWhileInProgress;

    MotionState fromMotionState = parentState.getMotionState();
    MotionState toMotionState = state.getMotionState();

    if (fromMotionState.isAtTerminal() && !toMotionState.isAtTerminal()) {
      return 1.0;
    }

    if (BlockStateTransitionModel.hasDestinationSignCodeChangedBetweenObservations(obs))
      return 0.95;

    return 0.05;
  }

  /*****
   * 
   ****/

  public double computeDeadheadBeforeProbabilities(VehicleState parentState,
      VehicleState state, Observation obs) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_BEFORE || blockState == null)
      return 1.0;

    /**
     * Rule: DEADHEAD_BEFORE => making progress towards start of the block
     */

    double pLongTermProgressTowardsStartOfBlock = computeLongRangeProgressTowardsStartOfBlockProbability(
        state, obs);

    double pShortTermProgressTowardsStartOfBlock = 1.0;
    if (parentState != null)
      pShortTermProgressTowardsStartOfBlock = computeShortRangeProgressTowardsStartOfBlockProbability(
          parentState, state, obs);

    double pProgressTowardsStartOfBlock = or(
        pLongTermProgressTowardsStartOfBlock,
        pShortTermProgressTowardsStartOfBlock);

    /**
     * Rule: DEADHEAD_BEFORE => start block on time
     */

    double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, obs);

    return pProgressTowardsStartOfBlock * pStartBlockOnTime;
  }

  /****
   * 
   ****/

  /**
   * Compute various probabilities concerning layovers
   * 
   * @param state
   * @param obs
   * @return
   */
  public double computeLayoverProbabilities(VehicleState state, Observation obs) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    BlockState blockState = state.getBlockState();

    /**
     * Rule: LAYOVER <=> Vehicle has not moved AND at layover location
     */

    double pNotMoved = computeVehicelHasNotMovedProbability(
        state.getMotionState(), obs);

    double pAtLayoverLocation = p(_vehicleStateLibrary.isAtPotentialLayoverSpot(
        state, obs));

    /**
     * LAYOVER_AFTER => ! pAtLayoverLocation
     */
    if (phase == EVehiclePhase.LAYOVER_AFTER)
      pAtLayoverLocation = not(pAtLayoverLocation);

    double pLayoverState = p(EVehiclePhase.isLayover(phase));

    double p1 = biconditional(pNotMoved * pAtLayoverLocation, pLayoverState);

    /**
     * Rule: LAYOVER_DURING => made some progress on the block
     */

    double pLayoverDuring = p(phase == EVehiclePhase.LAYOVER_DURING);
    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(blockState);

    double p2 = implies(pLayoverDuring, pServedSomePartOfBlock);

    /**
     * Rule: LAYOVER_BEFORE OR LAYOVER_DURING => vehicle_is_on_schedule
     */

    double p3 = 1.0;
    boolean isActiveLayoverState = EVehiclePhase.isActiveLayover(phase);

    if (isActiveLayoverState && blockState != null) {

      BlockStopTimeEntry nextStop = phase == EVehiclePhase.LAYOVER_BEFORE
          ? blockState.getBlockLocation().getNextStop()
          : _vehicleStateLibrary.getPotentialLayoverSpot(blockState.getBlockLocation());

      p3 = computeVehicleIsOnScheduleProbability(obs.getTime(), blockState,
          nextStop);
    }

    return p1 * p2 * p3;
  }

  /**
   * @return the probability that the vehicle has not moved in a while
   */
  public double computeVehicelHasNotMovedProbability(MotionState motionState,
      Observation obs) {

    long currentTime = obs.getTime();
    long lastInMotionTime = motionState.getLastInMotionTime();
    int secondsSinceLastMotion = (int) ((currentTime - lastInMotionTime) / 1000);

    if (120 <= secondsSinceLastMotion) {
      return 1.0;
    } else if (60 <= secondsSinceLastMotion) {
      return 0.9;
    } else {
      return 0.0;
    }
  }

  /*****
   * 
   ****/

  public double computeInProgressProbabilities(VehicleState parentState,
      VehicleState state, Observation obs) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

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
    double pOnRoute = computeOnRouteProbability(state, obs);

    /**
     * Rule: IN_PROGRESS => block location is close to gps location
     */
    double pBlockLocation = computeBlockLocationProbability(parentState,
        blockState, obs);

    return pNotMakingShortRangeProgressTowardsStartOfBlock * pOnRoute
        * pBlockLocation;
  }

  public double computeBlockLocationProbability(VehicleState parentState,
      BlockState blockState, Observation obs) {

    if (!_useBlockLocationDeviationModel)
      return 1.0;

    /**
     * The idea here is that we look for the absolute best block location given
     * our current oberservation, even if it means travelling backwards
     */
    BlockState closestBlockState = _blockStateTransitionModel.getClosestBlockState(
        blockState, obs);
    ScheduledBlockLocation closestBlockLocation = closestBlockState.getBlockLocation();

    /**
     * We compare this against our best block location assuming a bus generally
     * travels forward
     */
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    /**
     * If we're just coming out of a layover, there is some chance that the
     * block location was allowed to shift to the end of the layover to match
     * the underlying schedule and may be slightly ahead of our current block
     * location. We're ok with that.
     */
    if (parentState != null
        && EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())) {
      double delta = blockLocation.getDistanceAlongBlock()
          - closestBlockLocation.getDistanceAlongBlock();

      if (0 <= delta && delta < 300)
        return 1.0;
    }

    /**
     * If the distance between the two points is high, that means that our block
     * location isn't great and might suggest we've been assigned a block that
     * is moving in the wrong direction
     */
    double blockLocationDelta = SphericalGeometryLibrary.distance(
        closestBlockLocation.getLocation(), blockLocation.getLocation());
    return _blockLocationDeviationModel.probability(blockLocationDelta);
  }

  public double computeScheduleDeviationProbability(VehicleState state,
      Observation observation) {

    int delta = _scheduleDeviationLibrary.computeScheduleDeviation(state,
        observation);

    return _scheduleDeviationModel.probability(delta);
  }

  public double computeOnRouteProbability(VehicleState state, Observation obs) {

    double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
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

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_DURING || blockState == null)
      return 1.0;

    /**
     * Rule: DEADHEAD_DURING <=> Vehicle has moved AND at layover location
     */

    double pMoved = not(computeVehicelHasNotMovedProbability(
        state.getMotionState(), obs));

    double pAtLayoverLocation = p(_vehicleStateLibrary.isAtPotentialLayoverSpot(
        state, obs));

    double p1 = pMoved * pAtLayoverLocation;

    /**
     * Rule: DEADHEAD_DURING => not right on block
     */
    double pDistanceFromBlock = computeDeadheadDistanceFromBlockProbability(
        obs, blockState);

    /**
     * Rule: DEADHEAD_DURING => resume block on time
     */
    double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, obs);

    /**
     * Rule: DEADHEAD_DURING => served some part of block
     */
    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());

    return p1 * pDistanceFromBlock * pStartBlockOnTime * pServedSomePartOfBlock;
  }

  /****
   * 
   ****/

  public double computeDeadheadOrLayoverAfterProbabilities(VehicleState state,
      Observation obs) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    /**
     * Why do we check for a block state here? We assume a vehicle can't
     * deadhead or layover after unless it's actively served a block
     */
    if (!EVehiclePhase.isActiveAfterBlock(phase) || blockState == null)
      return 1.0;

    /**
     * RULE: DEADHEAD_AFTER || LAYOVER_AFTER => reached the end of the block
     */

    double pEndOfBlock = computeProbabilityOfEndOfBlock(state.getBlockState());

    /**
     * There is always some chance that the vehicle has served some part of the
     * block and then gone rogue: out-of-service or off-block. Unfortunately,
     * this is tricky because a vehicle could have been incorrectly assigned to
     * a block ever so briefly (fulfilling the served-some-part-of-the-block
     * requirement), it can quickly go off-block if the block assignment doesn't
     * keep up.
     */
    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
    double pOffBlock = computeOffBlockProbability(state, obs);
    double pOutOfService = computeOutOfServiceProbability(obs);

    double offRouteOrOutOfService = pServedSomePartOfBlock
        * or(pOffBlock, pOutOfService);

    return or(pEndOfBlock, offRouteOrOutOfService);
  }

  /****
   * 
   ****/

  public double computePriorProbability(VehicleState state) {

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

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

    JourneyState parentJourneyState = parentState.getJourneyState();
    JourneyState journeyState = state.getJourneyState();

    EVehiclePhase parentPhase = parentJourneyState.getPhase();
    EVehiclePhase phase = journeyState.getPhase();

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

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    CoordinatePoint location = blockLocation.getLocation();

    double d = SphericalGeometryLibrary.distance(location, obs.getLocation());

    if (d > 100)
      return 1.0;

    return 0.25;
  }

  public double computeLongRangeProgressTowardsStartOfBlockProbability(
      VehicleState state, Observation obs) {

    JourneyState js = state.getJourneyState();
    BlockState blockState = state.getBlockState();

    JourneyStartState startState = js.getData();
    CoordinatePoint journeyStartLocation = startState.getJourneyStart();

    CoordinatePoint currentLocation = obs.getLocation();

    // If we don't have a block assignment, are we really making progress?
    if (blockState == null)
      return 0.5;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    CoordinatePoint blockStart = blockLocation.getLocation();

    // Are we making progress towards the start of the block?
    double directDistance = SphericalGeometryLibrary.distance(
        journeyStartLocation, blockStart);
    double traveledDistance = SphericalGeometryLibrary.distance(
        currentLocation, journeyStartLocation);
    double remainingDistance = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);

    /**
     * This method only applies if we're already reasonable close to the start
     * of the block
     */
    if (remainingDistance <= _shortRangeProgressDistance)
      return 0.0;

    if (directDistance < 500) {
      double delta = remainingDistance - 500;
      return _travelToStartOfBlockDistanceModel.probability(delta);
    } else {
      // Ratio should be 1 if we drove directly from the depot to the start of
      // the
      // block but will increase as we go further out of our way
      double ratio = (traveledDistance + remainingDistance) / directDistance;

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

    BlockState blockState = state.getBlockState();

    /**
     * If we don't have a block state, we can't be making progress towards the
     * start
     */
    if (blockState == null)
      return 0.0;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    /**
     * If we don't have a parent state, we've just started and can be making
     * progress anywhere
     */
    if (parentState == null)
      return 1.0;

    /**
     * This method only applies if we haven't already started the block
     */
    EVehiclePhase parentPhase = parentState.getJourneyState().getPhase();
    if (!EVehiclePhase.isActiveBeforeBlock(parentPhase))
      return 0.0;

    /**
     * This method only applies if we're still at the beginning of the block
     */
    if (blockLocation.getDistanceAlongBlock() > 200)
      return 0.0;

    NycVehicleLocationRecord prev = obs.getPreviousRecord();
    CoordinatePoint prevLocation = new CoordinatePoint(prev.getLatitude(),
        prev.getLongitude());

    double dParent = SphericalGeometryLibrary.distance(prevLocation,
        blockLocation.getLocation());
    double dNow = _vehicleStateLibrary.getDistanceToBlockLocation(obs,
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

    NycVehicleLocationRecord record = observation.getRecord();
    String observedDsc = record.getDestinationSignCode();

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

  public double computeStartOrResumeBlockOnTimeProbability(VehicleState state,
      Observation obs) {

    CoordinatePoint currentLocation = obs.getLocation();

    BlockState blockState = state.getBlockState();

    // If we don't have an assigned block yet, we hedge our bets on whether
    // we're starting on time or not
    if (blockState == null)
      return 0.5;

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    CoordinatePoint blockStart = blockLocation.getLocation();

    // in meters
    double distanceToBlockStart = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);
    // in seconds
    int timeToStart = (int) (distanceToBlockStart / _averageSpeed);

    long estimatedStart = obs.getTime() + timeToStart * 1000;
    long scheduledStart = blockInstance.getServiceDate()
        + blockLocation.getScheduledTime() * 1000;

    int minutesDiff = (int) ((scheduledStart - estimatedStart) / (1000 * 60));

    // Arriving early?
    if (minutesDiff >= 0) {
      // Allow for a layover
      minutesDiff = Math.max(0, minutesDiff - _maxLayover);
    }

    return _startBlockOnTimeModel.probability(Math.abs(minutesDiff));
  }

  /**
   * The general idea is that a bus shouldn't be at its layover beyond its next
   * stop arrival time.
   * 
   * @param layoverStop
   */
  public double computeVehicleIsOnScheduleProbability(long timestamp,
      BlockState blockState, BlockStopTimeEntry layoverStop) {

    BlockInstance blockInstance = blockState.getBlockInstance();

    // No next stop? No point of a layover, methinks...
    if (layoverStop == null) {
      return 0.0;
    }

    StopTimeEntry stopTime = layoverStop.getStopTime();
    long arrivalTime = blockInstance.getServiceDate()
        + stopTime.getArrivalTime() * 1000;

    // If we still have time to spare, then no problem!
    if (timestamp < arrivalTime)
      return 1.0;

    int minutesLate = (int) ((timestamp - arrivalTime) / (60 * 1000));
    return _vehicleIsOnScheduleModel.probability(minutesLate);
  }

  public double computeOffBlockProbability(VehicleState state, Observation obs) {

    double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        obs, state.getBlockState());

    if (_offBlockDistance < distanceToBlock)
      return 1.0;
    else
      return 0.0;
  }

  public double computeProbabilityOfServingSomePartOfBlock(BlockState blockState) {

    // If we don't have a block, then we haven't made progress on a block
    if (blockState == null)
      return 0.0;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    // How much of a block do we need to run to consider ourselves started?
    if (blockLocation.getDistanceAlongBlock() > 500)
      return 1.0;
    else
      return 0.0;
  }

  public double computeProbabilityOfEndOfBlock(BlockState blockState) {

    // If we don't have a block, we can't be at the end of a block
    if (blockState == null)
      return 0.0;

    BlockInstance blockInstance = blockState.getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    double totalBlockDistance = blockConfig.getTotalBlockDistance();

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    double distanceAlongBlock = blockLocation.getDistanceAlongBlock();

    double delta = totalBlockDistance - distanceAlongBlock;
    return _endOfBlockDeviationModel.probability(delta);
  }

  public double computeOutOfServiceProbability(Observation observation) {

    NycVehicleLocationRecord record = observation.getRecord();
    String dsc = record.getDestinationSignCode();
    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);

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

}
