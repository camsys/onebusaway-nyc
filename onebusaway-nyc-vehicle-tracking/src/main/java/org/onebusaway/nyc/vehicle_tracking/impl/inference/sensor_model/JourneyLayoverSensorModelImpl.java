package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model.MotionModelLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeEntry;

/**
 * 
 * 
 * @author bdferris
 * 
 */
public class JourneyLayoverSensorModelImpl implements SensorModel<Observation> {

  /**
   * How long in minutes do we let a vehicle run past its next stop arrival
   * time?
   */
  private DeviationModel _vehicleIsOnScheduleModel = new DeviationModel(5);

  /**
   * How much do we let the bus drift during a layover? Distance in meters
   */
  private DeviationModel _vehicleHasNotMovedModel = new DeviationModel(10);

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();
    EdgeState edgeState = state.getEdgeState();
    MotionState motionState = state.getMotionState();
    BlockState blockState = state.getJourneyState().getBlockState();

    return likelihood(observation, edgeState, motionState, blockState);
  }

  public double likelihood(Observation observation, EdgeState edgeState,
      MotionState motionState, BlockState blockState) {

    double pVehicleIsAtLayover = getVehicleIsAtLayoverProbability(blockState);
    double pVehicleIsOnSchedule = getVehicleIsOnScheduleProbability(
        observation.getTime(), blockState);
    double pVehicleHasNotMoved = getVehicelHasNotMovedProbability(edgeState,
        motionState);

    return pVehicleIsAtLayover * pVehicleIsOnSchedule * pVehicleHasNotMoved;
  }

  public double getVehicleIsAtLayoverProbability(BlockState blockState) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    boolean isAtLayover = MotionModelLibrary.isAtPotentialLayoverSpot(blockLocation);

    if (isAtLayover)
      return 1.0;
    else
      return 0.1;
  }

  /**
   * The general idea is that a bus shouldn't be at its layover beyond its next
   * stop arrival time.
   */
  public double getVehicleIsOnScheduleProbability(long timestamp,
      BlockState blockState) {

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    BlockStopTimeEntry nextStop = blockLocation.getNextStop();
    
    // No next stop?  No point of a layover, methinks...
    if( nextStop == null) {
      return 0.0;
    }
    
    StopTimeEntry stopTime = nextStop.getStopTime();
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
  public double getVehicelHasNotMovedProbability(EdgeState edgeState,
      MotionState motionState) {

    CoordinatePoint currentLocation = edgeState.getLocationOnEdge();
    CoordinatePoint layoverLocation = motionState.getLastInMotionLocation();

    // Drift in distance should be zero if we really are in a layover
    double d = SphericalGeometryLibrary.distance(currentLocation,
        layoverLocation);
    return _vehicleHasNotMovedModel.probability(d);
  }
}
