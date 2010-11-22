package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel2;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleStateSensorModel {

  /****
   * Services
   ****/

  private VehicleStateLibrary _vehicleStateLibrary;

  private DestinationSignCodeService _destinationSignCodeService;

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

  private DeviationModel _tripLocationDeviationModel = new DeviationModel(20);

  private DeviationModel _scheduleDeviationModel = new DeviationModel(15 * 60);

  /**
   * What are the odds of switching blocks mid-stream? LOW!
   */
  private double _probabilityOfBlockChange = 0.01;

  private double _propabilityOfBeingOutOfServiceWithAnOutOfServiceDSC = 0.95;

  private double _propabilityOfBeingOutOfServiceWithAnInServiceDSC = 0.05;

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

  /****
   * {@link VehicleStateSensorModel} Interface
   ****/

  public double likelihood(VehicleState parentState, VehicleState state,
      Observation observation) {

    JourneyState journeyState = state.getJourneyState();

    switch (journeyState.getPhase()) {
      case AT_BASE:
        return likelihoodAtBase(state, observation);
      case DEADHEAD_BEFORE:
        return likelihoodDeadheadBefore(parentState, state, observation);
      case LAYOVER_BEFORE:
        return likelihoodLayoverBefore(state, observation);
      case IN_PROGRESS:
        return likelihoodInProgress(parentState, state, observation);
      case DEADHEAD_DURING:
        return likelihoodDeadheadDuring(state, observation);
      case LAYOVER_DURING:
        return likelihoodLayoverDuring(state, observation);
      case DEADHEAD_AFTER:
        return likelihoodDeadheadAfter(state, observation);
      case LAYOVER_AFTER:
        return likelihoodLayoverAfter(state, observation);
      case UNKNOWN:
        return likelihoodUnknown(state, observation);
    }

    throw new IllegalStateException("unknown journey state: "
        + journeyState.getPhase());
  }

  /****
   * Private Methods
   ****/

  private double likelihoodAtBase(VehicleState state, Observation observation) {

    EdgeState edgeState = state.getEdgeState();

    boolean isAtBase = _vehicleStateLibrary.isAtBase(edgeState);

    // You're either at a base or you're not
    return isAtBase ? 1.0 : 0.0;
  }

  private double likelihoodDeadheadBefore(VehicleState parentState,
      VehicleState state, Observation observation) {

    BlockState blockState = state.getBlockState();

    double pDistanceFromBlock = computeDeadheadDistanceFromBlockProbability(
        state.getEdgeState(), blockState);

    double pSignCode = computeDeadheadDestinationSignCodeProbability(
        blockState, observation);

    double pLongTermProgressTowardsStartOfBlock = computeLongRangeProgressTowardsStartOfBlockProbability(state);

    double pShortTermProgressTowardsStartOfBlock = 1.0;
    if (parentState != null)
      pShortTermProgressTowardsStartOfBlock = computeShortRangeProgressTowardsStartOfBlockProbability(
          parentState, state);

    double pProgressTowardsStartOfBlock = or(
        pLongTermProgressTowardsStartOfBlock,
        pShortTermProgressTowardsStartOfBlock);

    double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, observation);

    return pDistanceFromBlock * pSignCode * pProgressTowardsStartOfBlock
        * pStartBlockOnTime;
  }

  private double likelihoodLayoverBefore(VehicleState state,
      Observation observation) {

    MotionState motionState = state.getMotionState();
    BlockState blockState = state.getBlockState();

    double pNotAtBase = 1.0 - likelihoodAtBase(state, observation);

    double pVehicleHasNotMoved = computeVehicelHasNotMovedProbability(
        motionState, observation);

    /**
     * If we don't have a block yet, mostly just verify that we're not at a base
     * and we haven't moved
     */
    if (blockState == null)
      return pNotAtBase * pVehicleHasNotMoved;

    double pVehicleIsAtLayover = computeVehicleIsAtLayoverProbability(blockState);
    double pVehicleIsOnSchedule = computeVehicleIsOnScheduleProbability(
        observation.getTime(), blockState,
        blockState.getBlockLocation().getNextStop());

    return pNotAtBase * pVehicleHasNotMoved * pVehicleIsAtLayover
        * pVehicleIsOnSchedule;
  }

  private double likelihoodInProgress(VehicleState parentState,
      VehicleState state, Observation observation) {

    // If we don't have a block, we can't be in progress on a block
    if (state.getBlockState() == null)
      return 0.0;

    /**
     * As we get closer to the start of the block, we don't want to start the
     * block until we actually get there.
     */
    double pNotMakingShortRangeProgressTowardsStartOfBlock = 1.0;
    if (parentState != null)
      pNotMakingShortRangeProgressTowardsStartOfBlock = 1.0 - computeShortRangeProgressTowardsStartOfBlockProbability(
          parentState, state);

    /**
     * Penalize for deviation from the schedule
     */
    double pSchedule = computeScheduleDeviationProbability(state, observation);

    /**
     * Penalize for switching an in-progress block
     */
    double pBlockSwitch = computeBlockSwitchProbability(parentState, state,
        observation);

    double pLayoverBefore = likelihoodLayoverBefore(state, observation);
    double pLayoverDuring = likelihoodLayoverDuring(state, observation);

    /**
     * Penalize if we could be at a layover
     */
    double pNotAtLayover = 1.0 - or(pLayoverBefore, pLayoverDuring);

    /**
     * Penalize if we could be off route
     */
    double pOnRoute = computeOnRouteProbability(state);

    return pNotMakingShortRangeProgressTowardsStartOfBlock * pSchedule
        * pBlockSwitch * pNotAtLayover * pOnRoute;
  }

  private double likelihoodDeadheadDuring(VehicleState state,
      Observation observation) {

    BlockState blockState = state.getBlockState();

    // If we don't have a block, we can't be in progress on a block
    if (blockState == null)
      return 0.0;

    /**
     * Only allow deadhead during if we're at a layover location on the block
     */
    double pVehicleIsAtLayover = computeVehicleIsAtLayoverProbability(blockState);

    /**
     * Only allow deadhead during if we're not right on the block itself
     */
    double pDistanceFromBlock = computeDeadheadDistanceFromBlockProbability(
        state.getEdgeState(), blockState);

    double pSignCode = computeDeadheadDestinationSignCodeProbability(
        blockState, observation);

    double pStartBlockOnTime = computeStartOrResumeBlockOnTimeProbability(
        state, observation);

    /**
     * Only allow deadhead during block if we've actually started the block
     */
    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());

    return pVehicleIsAtLayover * pDistanceFromBlock * pSignCode
        * pStartBlockOnTime * pServedSomePartOfBlock;
  }

  private double likelihoodLayoverDuring(VehicleState state,
      Observation observation) {

    MotionState motionState = state.getMotionState();
    BlockState blockState = state.getBlockState();

    // If we don't have a block, we can't be in progress on a block
    if (blockState == null)
      return 0.0;

    BlockStopTimeEntry layoverStop = _vehicleStateLibrary.getPotentialLayoverSpot(blockState.getBlockLocation());

    if (layoverStop == null)
      return 0.0;

    double pVehicleIsOnSchedule = computeVehicleIsOnScheduleProbability(
        observation.getTime(), blockState, layoverStop);

    double pVehicleHasNotMoved = computeVehicelHasNotMovedProbability(
        motionState, observation);

    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());

    return pVehicleIsOnSchedule * pVehicleHasNotMoved * pServedSomePartOfBlock;
  }

  private double likelihoodDeadheadAfter(VehicleState state,
      Observation observation) {

    /**
     * Only allow deadhead after if we've reached the end of the block OR (we've
     * served some part of the block and now we've REALLY deviated from our
     * route OR we're out-of-service). Plus, we need to be moving
     */

    double pEndOfBlock = computeProbabilityOfEndOfBlock(state.getBlockState());

    double pServedSomePartOfBlock = computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
    double pOffBlock = computeOffBlockProbability(state);
    double pOutOfService = computeOutOfServiceProbability(observation);

    double offRouteOrOutOfService = pServedSomePartOfBlock
        * or(pOffBlock, pOutOfService);

    double pVehicleHasMoved = 1.0 - computeVehicelHasNotMovedProbability(
        state.getMotionState(), observation);

    return or(pEndOfBlock, offRouteOrOutOfService) * pVehicleHasMoved;
  }

  private double likelihoodLayoverAfter(VehicleState state,
      Observation observation) {

    /**
     * Only allow layover after if we've completed a block and we're not moving
     */

    double pEndOfBlock = computeProbabilityOfEndOfBlock(state.getBlockState());
    double pVehicleHasNotMoved = computeVehicelHasNotMovedProbability(
        state.getMotionState(), observation);

    return pEndOfBlock * pVehicleHasNotMoved;
  }

  private double likelihoodUnknown(VehicleState state, Observation observation) {

    NycVehicleLocationRecord record = observation.getRecord();
    String dsc = record.getDestinationSignCode();
    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);

    if (outOfService)
      return _propabilityOfUnknownWithAnOutOfServiceDSC;

    // TODO : How do we measure this?
    return 1.0;
  }

  /****
   * Various Probabilities
   ****/

  /**
   * Deadheading is only likely if we aren't super closer to the block path
   * itself
   */
  public double computeDeadheadDistanceFromBlockProbability(
      EdgeState edgeState, BlockState blockState) {

    // If we don't have a block, we could go either way
    if (blockState == null)
      return 0.5;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    CoordinatePoint location = blockLocation.getLocation();

    double d = SphericalGeometryLibrary.distance(location,
        edgeState.getLocationOnEdge());

    if (d > 100)
      return 1.0;

    return 0.25;
  }

  public double computeLongRangeProgressTowardsStartOfBlockProbability(
      VehicleState state) {

    EdgeState edgeState = state.getEdgeState();
    JourneyState js = state.getJourneyState();
    BlockState blockState = state.getBlockState();

    JourneyStartState startState = js.getData();
    CoordinatePoint journeyStartLocation = startState.getJourneyStart();

    CoordinatePoint currentLocation = edgeState.getPointOnEdge().toCoordinatePoint();

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
   * @return
   */
  public double computeShortRangeProgressTowardsStartOfBlockProbability(
      VehicleState parentState, VehicleState state) {

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

    EdgeState parentEdge = parentState.getEdgeState();
    EdgeState edgeState = state.getEdgeState();

    double dParent = _vehicleStateLibrary.getDistanceToBlockLocation(
        parentEdge, blockState);
    double dNow = _vehicleStateLibrary.getDistanceToBlockLocation(edgeState,
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

    CoordinatePoint currentLocation = state.getEdgeState().getPointOnEdge().toCoordinatePoint();

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

  public double computeVehicleIsAtLayoverProbability(BlockState blockState) {

    // If we don't have a block, we can't be at a layover
    if (blockState == null)
      return 0.0;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    boolean isAtLayover = _vehicleStateLibrary.isAtPotentialLayoverSpot(blockLocation);

    if (isAtLayover)
      return 1.0;
    else
      return 0.1;
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

  /**
   * The general idea is that a bus shouldn't move during a layover
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

  public double computeStreetLocationVsBlockLocationProbability(
      VehicleState state) {

    BlockState blockState = state.getBlockState();

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = state.getEdgeState().getPointOnEdge();

    double distance = SphericalGeometryLibrary.distance(p1.getLat(),
        p2.getLon(), p2.getLat(), p2.getLon());
    return _tripLocationDeviationModel.probability(distance);
  }

  public double computeScheduleDeviationProbability(VehicleState state,
      Observation observation) {

    BlockState blockState = state.getBlockState();

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    long scheduledTime = blockInstance.getServiceDate()
        + blockLocation.getScheduledTime() * 1000;

    NycVehicleLocationRecord record = observation.getRecord();

    long delta = Math.abs(scheduledTime - record.getTime()) / 1000;

    return _scheduleDeviationModel.probability(delta);
  }

  public double computeBlockSwitchProbability(VehicleState parentState,
      VehicleState state, Observation obs) {

    if (parentState == null)
      return 1.0;

    BlockState parentBlockState = parentState.getBlockState();
    BlockState blockState = state.getBlockState();

    if (parentBlockState == null)
      return 1.0;

    if (parentBlockState.getBlockInstance().equals(
        blockState.getBlockInstance()))
      return 1.0;
    
    EVehiclePhase fromPhase = parentState.getJourneyState().getPhase();
    EVehiclePhase toPhase = state.getJourneyState().getPhase();
    
    if( EVehiclePhase.LAYOVER_AFTER == fromPhase && EVehiclePhase.LAYOVER_AFTER != toPhase)
      return 1.0;
    
    // The block changed!
    //System.out.println("blockFrom=" + parentBlockState.getBlockInstance());
    //System.out.println("blockTo=" + blockState.getBlockInstance());

    return _probabilityOfBlockChange;
  }

  public double computeOnRouteProbability(VehicleState state) {

    double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        state.getEdgeState(), state.getBlockState());

    if (distanceToBlock <= _offBlockDistance)
      return 1.0;
    else
      return 0.0;
  }

  public double computeOffBlockProbability(VehicleState state) {

    double distanceToBlock = _vehicleStateLibrary.getDistanceToBlockLocation(
        state.getEdgeState(), state.getBlockState());

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

  public static double or(double... pValues) {
    if (pValues.length == 0)
      return 0.0;
    double p = pValues[0];
    for (int i = 1; i < pValues.length; i++)
      p = p + pValues[i] - (p * pValues[i]);
    return p;
  }
}
