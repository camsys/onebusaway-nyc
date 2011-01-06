package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockStateTransitionModel {

  private DestinationSignCodeService _destinationSignCodeService;

  private BlocksFromObservationService _blocksFromObservationService;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  private ObservationCache _observationCache;

  /****
   * Parameters
   ****/

  /**
   * What are the odds that we'll allow a block switch when switching between
   * valid DSCs? Low.
   */
  private double _probabilityOfBlockSwitchForValidDscSwitch = 0.05;

  /**
   * Given a potential distance to travel in physical / street-network space, a
   * fudge factor that determines how far ahead we will look along the block of
   * trips in distance to travel
   */
  private double _blockDistanceTravelScale = 1.5;

  /****
   * Setters
   ****/

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

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

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  /****
   * Block State Methods
   ****/

  public BlockState transitionBlockState(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    String dsc = obs.getRecord().getDestinationSignCode();
    boolean outOfService = dsc != null
        && _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);

    BlockState blockState = parentState.getBlockState();

    EVehiclePhase parentPhase = parentState.getJourneyState().getPhase();

    boolean allowBlockChange = allowBlockTransition(parentState, motionState,
        journeyState, obs);

    /**
     * Do we explicitly allow a block change? If not, we just advance along our
     * current block.
     * 
     * Note that we are considering the parentPhase, not the newly proposed
     * phase here. I think that makes sense, since the proposed journeyPhase is
     * just an iteration over all phases with no real consideration of
     * likelihood yet.
     */
    if (!allowBlockChange) {

      if (EVehiclePhase.isActiveDuringBlock(parentPhase)) {

        /**
         * Update our block location along the block when we're actively serving
         * the block
         */
        return advanceAlongBlock(parentPhase, blockState, obs);

      } else if (EVehiclePhase.isActiveAfterBlock(parentPhase)) {

        if (blockState == null)
          return blockState;

        /**
         * Typically, we've finished up the block at this point and our block
         * state won't change. However, it's possible for us to go out of
         * service mid-block. In this situation we want to do our best to keep a
         * reasonable block match, both to detect how far off-block we've gone
         * or to correctly re-associate back on-block if need be.
         * 
         * Except that there is a performance hit for doing this AND it seems to
         * break things.
         * 
         * TODO: Examine whether the tradeoff is worth it
         */

        // return getClosestBlockState(blockState, obs);
        return blockState;

      } else if (parentPhase.equals(EVehiclePhase.AT_BASE)) {

        /**
         * If we're at the base and our current block state has been run to
         * completion, it's ok to forget about the block at this point
         */
        if (outOfService && blockState != null) {

          BlockInstance blockInstance = blockState.getBlockInstance();
          BlockConfigurationEntry blockConfig = blockInstance.getBlock();
          ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

          double remaining = blockConfig.getTotalBlockDistance()
              - blockLocation.getDistanceAlongBlock();

          if (remaining < 700)
            return null;
        }

        return blockState;

      } else {

        /**
         * Just keep our existing block location for everything else
         */
        return blockState;
      }
    }

    /**
     * We are allowing a block change. How we change the block is dependent on
     * the phase.
     */
    if (EVehiclePhase.isActiveBeforeBlock(parentPhase)) {

      if (outOfService) {
        /**
         * Operator just went out of service... let's just keep our current best
         * guess as to our sign code, or optionally disassociate our block
         */
        if (Math.random() < 0.5)
          return blockState;
        else
          return null;
      } else {
        /**
         * Since we haven't started our block yet, we allow a lot of flexibility
         * in switching to a new block
         */
        CDFMap<BlockState> cdf = _blockStateSamplingStrategy.cdfForJourneyAtStart(obs);
        if( cdf.isEmpty() )
          return null;
        return cdf.sample();
      }

    } else if (EVehiclePhase.isActiveDuringBlock(parentPhase)) {

      if (outOfService) {
        /**
         * Operator just went out of service... let's just keep our current best
         * guess as to our sign code.
         */
        return blockState;
      }

      /**
       * If we've actively started serving a block, we allow a lot less
       * flexibility in switching to a new block
       */
      boolean allowBlockChangeWhileInProgress = Math.random() < _probabilityOfBlockSwitchForValidDscSwitch;

      if (allowBlockChangeWhileInProgress) {
        CDFMap<BlockState> cdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(obs);
        if (cdf.isEmpty())
          return null;
        return cdf.sample();
      }

      /**
       * Otherwise we just advance along the current block
       */
      return advanceAlongBlock(parentPhase, blockState, obs);

    } else {

      /**
       * We've completed our block. If we've switch to out-of-service, hold onto
       * our old block
       */
      if (outOfService)
        return blockState;

      /**
       * If we've switch to another valid DSC, let's try switching the block.
       * But the question: are we starting a new block or resuming a block
       * already in progress?
       * 
       */

      if (Math.random() < 0.5) {
        CDFMap<BlockState> cdf = _blockStateSamplingStrategy.cdfForJourneyAtStart(obs);
        if (cdf.isEmpty())
          return null;
        return cdf.sample();
      } else {
        CDFMap<BlockState> cdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(obs);
        if (cdf.isEmpty())
          return null;
        return cdf.sample();
      }

    }
  }

  public BlockState getClosestBlockState(BlockState blockState, Observation obs) {

    Map<BlockInstance, BlockState> states = _observationCache.getValueForObservation(
        obs, EObservationCacheKey.CLOSEST_BLOCK_LOCATION);

    if (states == null) {
      states = new HashMap<BlockInstance, BlockState>();
      _observationCache.putValueForObservation(obs,
          EObservationCacheKey.CLOSEST_BLOCK_LOCATION, states);
    }

    BlockInstance blockInstance = blockState.getBlockInstance();

    BlockState closestState = states.get(blockInstance);

    if (closestState == null) {

      closestState = getClosestBlockStateUncached(blockState, obs);

      states.put(blockInstance, closestState);
    }

    return closestState;
  }

  /****
   * Private Methods
   ****/

  private boolean allowBlockTransition(VehicleState parentState,
      MotionState motionState, JourneyState journeyState, Observation obs) {

    /**
     * Have we just transitioned out of a terminal?
     */
    if (parentState != null) {
      MotionState parentMotionState = parentState.getMotionState();

      // If we just move out of the terminal, allow a block transition
      if (parentMotionState.isAtTerminal() && !motionState.isAtTerminal()) {
        return true;
      }
    }

    NycVehicleLocationRecord record = obs.getRecord();
    String dsc = record.getDestinationSignCode();

    boolean unknownDSC = _destinationSignCodeService.isUnknownDestinationSignCode(dsc);

    /**
     * If we have an unknown DSC, we don't allow a block change. What about
     * "0000"? 0000 is no longer considered an unknown DSC, so it should pass by
     * the unknown DSC check, and to the next check, where we only allow a block
     * change if we've changed from a good DSC.
     */
    if (unknownDSC)
      return false;

    /**
     * If the destination sign code has changed, we allow a block transition
     */
    if (hasDestinationSignCodeChangedBetweenObservations(obs))
      return true;

    return false;
  }

  private BlockState advanceAlongBlock(EVehiclePhase phase,
      BlockState blockState, Observation obs) {

    Observation prevObs = obs.getPreviousObservation();

    double distanceToTravel = 300
        + SphericalGeometryLibrary.distance(prevObs.getLocation(),
            obs.getLocation()) * _blockDistanceTravelScale;

    /**
     * TODO: Should we allow a vehicle to travel backwards?
     */
    BlockState updatedBlockState = _blocksFromObservationService.advanceState(
        obs, blockState, 0, distanceToTravel);

    if (EVehiclePhase.isLayover(phase)) {
      updatedBlockState = _blocksFromObservationService.advanceLayoverState(
          obs.getTime(), blockState);
    }

    return updatedBlockState;
  }

  private BlockState getClosestBlockStateUncached(BlockState blockState,
      Observation obs) {

    BlockState closestState;
    double distanceToTravel = 400;

    Observation prevObs = obs.getPreviousObservation();

    if (prevObs != null) {

      distanceToTravel = Math.max(
          distanceToTravel,
          SphericalGeometryLibrary.distance(obs.getLocation(),
              prevObs.getLocation())
              * _blockDistanceTravelScale);
    }

    closestState = _blocksFromObservationService.advanceState(obs, blockState,
        -distanceToTravel, distanceToTravel);

    ScheduledBlockLocation blockLocation = closestState.getBlockLocation();
    double d = SphericalGeometryLibrary.distance(blockLocation.getLocation(),
        obs.getLocation());

    if (d > 400) {
      closestState = _blocksFromObservationService.bestState(obs, blockState);
    }

    return closestState;
  }

  public static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    NycVehicleLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    return !ObjectUtils.equals(previouslyObservedDsc, observedDsc);
  }

  public static class BlockStateResult {

    public BlockStateResult(BlockState blockState, boolean outOfService) {
      this.blockState = blockState;
      this.outOfService = outOfService;
    }

    public BlockState blockState;
    public boolean outOfService;
  }
}
