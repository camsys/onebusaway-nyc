package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import java.util.Random;
import java.util.Set;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateSamplingStrategy;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlocksFromObservationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class JourneyInProgressMotionModel implements JourneyMotionModel {

  private static Logger _log = LoggerFactory.getLogger(JourneyInProgressMotionModel.class);

  private static Random _random = new Random();

  private BlocksFromObservationService _blocksFromObservationService;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  /**
   * Given a potential distance to travel in physical / street-network space, a
   * fudge factor that determines how far ahead we will look along the block of
   * trips in distance to travel
   */
  private double _blockDistanceTravelScale = 1.5;

  /**
   * At a top speed of 70 mph (31.2928 m/s), we want 20 mph (8.9408 m/s) of
   * variance.
   */
  private double _velocitySigmaScalar = 4.4704 / 31.2928;

  /**
   * What are the odds that we'll allow a block switch when switching between
   * valid DSCs? Low.
   */
  private double _probabilityOfBlockSwitchForValidDscSwitch = 0.05;

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategy blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  public void setVelocitySigmaScalar(double velocitySigmaScalar) {
    _velocitySigmaScalar = velocitySigmaScalar;
  }

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  /****
   * {@link MotionModel} Interface
   ****/

  @Override
  public JourneyState move(Particle parent, VehicleState parentState,
      EdgeState edgeState, MotionState motionState, Observation obs,
      JourneyState parentJourneyState) {

    double distanceToTravel = sampleDistanceToTravel(parent, parentState, obs);

    BlockState blockState = determineBlockInstance(parentState, obs, edgeState,
        distanceToTravel);

    // If we're at a layover transition point, consider going into START mode,
    // as the layover location might be off the block
    if (MotionModelLibrary.isAtPotentialLayoverSpot(blockState.getBlockLocation())) {
      if (Math.random() < 0.5)
        return JourneyState.deadheadBefore(blockState, edgeState.getLocationOnEdge());
    }

    if (MotionModelLibrary.isAtEndOfBlock(blockState)) {
      if (Math.random() < 0.75)
        return JourneyState.deadheadAfter(blockState);
    }

    // If we've been stopped for a while, consider a layover
    int stoppedTime = (int) ((obs.getTime() - motionState.getLastInMotionTime()) / (60 * 1000));
    if (stoppedTime > 5 && Math.random() < 0.5)
      return JourneyState.layover(blockState);

    JourneyState state = JourneyState.inProgress(blockState);

    if (parent != null) {
      VehicleState prev = parent.getData();
      BlockInstance b1 = prev.getJourneyState().getBlockState().getBlockInstance();
      BlockInstance b2 = state.getBlockState().getBlockInstance();
      if ((b1 == null && b2 != null) || !b1.equals(b2)) {
        _log.info("block change");
        _log.info("from=" + b1);
        _log.info("to=" + b2);
      }
    }

    return state;
  }

  private double sampleDistanceToTravel(Particle parent,
      VehicleState parentState, Observation obs) {

    EdgeState edgeState = parentState.getEdgeState();
    ProjectedPoint currentLocation = edgeState.getPointOnEdge();

    // Our strategy is to move towards the next gps point (with some
    // probability)
    ProjectedPoint targetPoint = obs.getPoint();

    // In meters
    double straightLineDistance = currentLocation.distance(targetPoint);

    // In seconds
    double ellapsedTime = (obs.getTime() - parent.getTimestamp()) / 1000.0;

    if (ellapsedTime == 0)
      return 0;

    // In meters / second
    double targetVelocity = straightLineDistance / ellapsedTime;

    double targetNoiseVelocity = targetVelocity * _velocitySigmaScalar;

    double sampledNoiseVelocity = _random.nextGaussian() * targetNoiseVelocity;

    // In meters
    return straightLineDistance + sampledNoiseVelocity * ellapsedTime;
  }

  private BlockState determineBlockInstance(VehicleState parentState,
      Observation obs, EdgeState edgeState, double distanceToTravel) {

    BlockState blockState = parentState.getJourneyState().getBlockState();

    String previouslyObservedDsc = null;
    NycVehicleLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    boolean sameDsc = previouslyObservedDsc != null
        && previouslyObservedDsc.equals(observedDsc);

    boolean allowBlockChange = _random.nextDouble() < _probabilityOfBlockSwitchForValidDscSwitch;

    // If the DSC hasn't changed,
    if (sameDsc || !allowBlockChange) {
      return _blocksFromObservationService.advanceState(record.getTime(),
          edgeState.getPointOnEdge(), blockState, distanceToTravel
              * _blockDistanceTravelScale);
    }

    _log.info("previousDSC=" + previouslyObservedDsc);
    _log.info("nextDSC=" + observedDsc);

    Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);
    return _blockStateSamplingStrategy.cdfForJourneyInProgress(instances, obs).sample();
  }
}
