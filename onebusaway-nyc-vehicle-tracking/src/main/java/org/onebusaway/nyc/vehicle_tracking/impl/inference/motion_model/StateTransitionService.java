package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateSamplingStrategy;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlocksFromObservationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StateTransitionService {

  private DestinationSignCodeService _destinationSignCodeService;

  private BlocksFromObservationService _blocksFromObservationService;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  private BaseLocationService _baseLocationService;

  /****
   * Parameters
   ****/

  private double _probabilityOfGoingFromOutOfServiceToInServiceWithDscChange = 0.75;

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
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  /****
   * 
   ****/

  public boolean isAtBase(EdgeState edgeState) {
    String baseName = _baseLocationService.getBaseNameForLocation(edgeState.getLocationOnEdge());
    return baseName != null;
  }

  public JourneyState transitionToBase(BlockState currentBlockState,
      Observation obs) {

    // Update block state
    BlockState blockState = updateBlockStateAtJourneyStart(currentBlockState,
        obs);

    return JourneyState.atBase(blockState);
  }

  public JourneyState potentiallyStartBlock(EdgeState edgeState,
      BlockState blockState, Observation obs, CoordinatePoint journeyStart) {

    // Update block state as necessary
    blockState = updateBlockStateAtJourneyStart(blockState, obs);

    double probOfStartingBlock = getProbabilityOfStartingBlock(edgeState,
        blockState);

    if (Math.random() <= probOfStartingBlock) {
      if (Math.random() < 0.5)
        return JourneyState.layover(blockState);
      else
        return JourneyState.inProgress(blockState);
    } else {

      // Maybe we are back at the base?
      if (isAtBase(edgeState))
        return JourneyState.atBase(blockState);

      return JourneyState.deadheadBefore(blockState, journeyStart);
    }
  }

  public JourneyState potentiallyStartOrContinueBlock(EdgeState edgeState,
      Observation obs) {

    Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);

    // Are we starting a whole new block? Or just picking up one already in
    // progress? Hard to tell, so we flip a coin to decide.
    if (Math.random() < 0.5) {

      // Start a whole new block
      BlockState blockState = _blockStateSamplingStrategy.sampleBlockStateAtJourneyStart(
          instances, obs);
      return potentiallyStartBlock(edgeState, blockState, obs,
          edgeState.getLocationOnEdge());

    } else {
      // Resume a block already in progress
      BlockState blockState = _blockStateSamplingStrategy.cdfForJourneyInProgress(instances, obs).sample();
      return JourneyState.inProgress(blockState);
    }
  }

  public BlockState updateBlockStateAtJourneyStart(BlockState blockState,
      Observation obs) {

    // If the DSC has changed, we potentially switch out target block instance
    if (MotionModelLibrary.hasDestinationSignCodeChangedBetweenObservations(obs)) {
      if (_destinationSignCodeService.isOutOfServiceDestinationSignCode(obs.getRecord().getDestinationSignCode())) {
        return blockState;
        // Operator just went out of service... let's just keep our current best
        // guess as to our sign code
      } else {
        Set<BlockInstance> instances = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);
        return _blockStateSamplingStrategy.sampleBlockStateAtJourneyStart(
            instances, obs, blockState);
      }
    }

    return blockState;
  }

  public boolean shouldWeGoBackInService(Observation obs) {

    // If the DSC hasn't changed, we stay out of service
    if (!MotionModelLibrary.hasDestinationSignCodeChangedBetweenObservations(obs))
      return false;

    NycVehicleLocationRecord record = obs.getRecord();

    // If we're out of service according to our DSC, we stay that way
    if (_destinationSignCodeService.isOutOfServiceDestinationSignCode(record.getDestinationSignCode()))
      return false;

    // The DSC has changed to something NOT out-of-service.

    // Only go back in service with some probability
    return Math.random() < _probabilityOfGoingFromOutOfServiceToInServiceWithDscChange;
  }

  /****
   * Private Methods
   ****/

  private double getProbabilityOfStartingBlock(EdgeState edgeState,
      BlockState blockState) {

    // Are we getting close to the start of a block?
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint currentLocation = edgeState.getPointOnEdge().toCoordinatePoint();
    CoordinatePoint blockStart = blockLocation.getLocation();

    double distanceToStartOfBlock = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);

    if (distanceToStartOfBlock < 100)
      return 0.9;

    return 0.05;
  }

}
