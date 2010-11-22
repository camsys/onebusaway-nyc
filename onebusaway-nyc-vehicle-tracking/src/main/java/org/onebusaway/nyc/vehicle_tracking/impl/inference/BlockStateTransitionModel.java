package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Set;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
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

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  /****
   * Block State Methods
   ****/

  public BlockState transitionBlockState(VehicleState parentState,
      EdgeState edgeState, MotionState motionState, JourneyState journeyState,
      Observation obs) {

    String dsc = obs.getRecord().getDestinationSignCode();
    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);
    boolean unknownDsc = _destinationSignCodeService.isUnknownDestinationSignCode(dsc);

    EdgeState prevEdgeState = parentState.getEdgeState();
    BlockState blockState = parentState.getBlockState();

    EVehiclePhase parentPhase = parentState.getJourneyState().getPhase();

    boolean phaseTransitionSuggestsBlockTransition = getPhaseTransitionSuggestsBlockTransition(
        parentState, journeyState);

    /**
     * If our DSC has not changed or we have an unknown DSC, we don't concern
     * ourselves with switching blocks, but we still might need to update our
     * block location
     */
    if (!phaseTransitionSuggestsBlockTransition
        && (!hasDestinationSignCodeChangedBetweenObservations(obs) || unknownDsc)) {

      if (EVehiclePhase.isActiveDuringBlock(parentPhase)) {

        /**
         * Update our block location along the block when we're actively serving
         * the block
         */
        return advanceAlongBlock(prevEdgeState, edgeState, blockState, obs);

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
     * The destination sign code HAS changed, which is a strong indicator that
     * we might be updating our block or picking a new block completely. How we
     * handle that is determined by which journey phase we're in.
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
        Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);
        return _blockStateSamplingStrategy.sampleBlockStateAtJourneyStart(
            instances, obs, blockState, phaseTransitionSuggestsBlockTransition);
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
      boolean allowBlockChange = Math.random() < _probabilityOfBlockSwitchForValidDscSwitch;

      if (allowBlockChange) {
        Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);
        return _blockStateSamplingStrategy.cdfForJourneyInProgress(instances,
            obs).sample();
      }

      /**
       * Otherwise we just advance along the current block
       */
      return advanceAlongBlock(prevEdgeState, edgeState, blockState, obs);

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

      Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);

      if (Math.random() < 0.5) {
        
        return _blockStateSamplingStrategy.sampleBlockStateAtJourneyStart(
            instances, obs, blockState, phaseTransitionSuggestsBlockTransition);
      } else {
        return _blockStateSamplingStrategy.cdfForJourneyInProgress(instances,
            obs).sample();
      }

    }
  }

  private boolean getPhaseTransitionSuggestsBlockTransition(
      VehicleState parentState, JourneyState journeyState) {

    if (parentState == null || journeyState == null)
      return false;

    EVehiclePhase from = parentState.getJourneyState().getPhase();
    EVehiclePhase to = journeyState.getPhase();

    /**
     * If we've transitioned from layover_after to deadhead_after, the driver
     * might not have entered a valid DSC switch, so we check anyway
     */
    return EVehiclePhase.LAYOVER_AFTER == from
        && EVehiclePhase.LAYOVER_AFTER != to;
  }

  private BlockState advanceAlongBlock(EdgeState prevEdgeState,
      EdgeState edgeState, BlockState blockState, Observation obs) {

    double distanceToTravel = SphericalGeometryLibrary.distance(
        prevEdgeState.getLocationOnEdge(), edgeState.getLocationOnEdge());

    return _blocksFromObservationService.advanceState(obs.getTime(),
        edgeState.getPointOnEdge(), blockState, distanceToTravel
            * _blockDistanceTravelScale);
  }

  private static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    NycVehicleLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    return previouslyObservedDsc == null
        || !previouslyObservedDsc.equals(observedDsc);
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
