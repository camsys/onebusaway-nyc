package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * We model the idea that a vehicle leaving the depot should generally be
 * driving towards the start of its block.
 * 
 * @author bdferris
 * 
 */
public class JourneyDeadheadBeforeSensorModelImpl implements SensorModel<Observation> {

  private DestinationSignCodeService _destinationSignCodeService;

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

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();

    BlockState blockState = state.getJourneyState().getBlockState();

    double pSignCode = getDestinationSignCodeProbability(blockState,
        observation);

    double pProgressTowardsStartOfBlock = getProgressTowardsStartOfBlockProbability(state);

    double pStartBlockOnTime = getStartBlockOnTimeProbability(state,
        observation);

    return pSignCode * pProgressTowardsStartOfBlock * pStartBlockOnTime;
  }

  private double getProgressTowardsStartOfBlockProbability(VehicleState state) {

    EdgeState edgeState = state.getEdgeState();
    JourneyState js = state.getJourneyState();
    BlockState blockState = state.getJourneyState().getBlockState();

    JourneyStartState startState = js.getData();
    CoordinatePoint journeyStartLocation = startState.getJourneyStart();

    CoordinatePoint currentLocation = edgeState.getPointOnEdge().toCoordinatePoint();

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    
    if( blockLocation == null)
      return 0.5;
    
    CoordinatePoint blockStart = blockLocation.getLocation();

    // Are we making progress towards the start of the block?
    double directDistance = SphericalGeometryLibrary.distance(
        journeyStartLocation, blockStart);
    double traveledDistance = SphericalGeometryLibrary.distance(
        currentLocation, journeyStartLocation);
    double remainingDistance = SphericalGeometryLibrary.distance(
        currentLocation, blockStart);

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

  public double getDestinationSignCodeProbability(BlockState blockState,
      Observation observation) {

    NycVehicleLocationRecord record = observation.getRecord();
    String observedDsc = record.getDestinationSignCode();
    String blockDsc = blockState.getDestinationSignCode();

    // If the driver hasn't set an in-service DSC yet, we can't punish too much
    if (_destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc))
      return 0.5;

    // We do have an in-service DSC at this point, so we heavily favor blocks
    // that have the same DSC
    if (observedDsc.equals(blockDsc)) {
      return 0.95;
    } else {
      return 0.05;
    }
  }

  public double getStartBlockOnTimeProbability(VehicleState state,
      Observation obs) {

    CoordinatePoint currentLocation = state.getEdgeState().getPointOnEdge().toCoordinatePoint();

    BlockState blockState = state.getJourneyState().getBlockState();
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
}
