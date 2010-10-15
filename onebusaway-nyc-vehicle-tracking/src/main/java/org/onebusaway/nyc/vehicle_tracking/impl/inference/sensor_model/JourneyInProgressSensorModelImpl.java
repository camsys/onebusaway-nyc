package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class JourneyInProgressSensorModelImpl implements
    SensorModel<Observation> {

  private DeviationModel _tripLocationDeviationModel = new DeviationModel(20);

  private DeviationModel _scheduleDeviationModel = new DeviationModel(15 * 60);

  /**
   * What are the odds of switching blocks mid-stream? LOW!
   */
  private double _probabilityOfBlockChange = 0.001;

  /**
   * 
   * @param tripLocationSigma in meters
   */
  public void setTripLocationSigma(double tripLocationSigma) {
    _tripLocationDeviationModel = new DeviationModel(tripLocationSigma);
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  public void setScheduleDeviationSigma(double scheduleDeviationSigma) {
    _scheduleDeviationModel = new DeviationModel(scheduleDeviationSigma * 60);
  }

  @Override
  public double likelihood(Particle particle, Observation obs) {

    // Penalize for distance away from trip path
    double pTrip = computeStreetLocationVsBlockLocationProbability(particle);

    // Penalize for deviation from the schedule
    double pSchedule = computeScheduleDeviationProbability(particle, obs);

    double pBlockSwitch = computeBlockSwitchProbability(particle, obs);

    return pTrip * pSchedule * pBlockSwitch;
  }

  public double computeStreetLocationVsBlockLocationProbability(
      Particle particle) {

    VehicleState state = particle.getData();
    JourneyState journeyState = state.getJourneyState();
    BlockState blockState = journeyState.getBlockState();

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = state.getEdgeState().getPointOnEdge();

    double distance = SphericalGeometryLibrary.distance(p1.getLat(),
        p2.getLon(), p2.getLat(), p2.getLon());
    return _tripLocationDeviationModel.probability(distance);
  }

  public double computeScheduleDeviationProbability(Particle particle,
      Observation observation) {

    VehicleState state = particle.getData();
    JourneyState journeyState = state.getJourneyState();
    BlockState blockState = journeyState.getBlockState();

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    long scheduledTime = blockInstance.getServiceDate()
        + blockLocation.getScheduledTime() * 1000;

    NycVehicleLocationRecord record = observation.getRecord();

    long delta = Math.abs(scheduledTime - record.getTime()) / 1000;

    return _scheduleDeviationModel.probability(delta);
  }

  private double computeBlockSwitchProbability(Particle particle,
      Observation obs) {

    Particle parent = particle.getParent();

    if (parent == null)
      return 1.0;

    VehicleState parentState = parent.getData();
    VehicleState state = particle.getData();

    BlockState parentBlockState = parentState.getJourneyState().getBlockState();
    BlockState blockState = state.getJourneyState().getBlockState();

    if (parentBlockState.getBlockInstance() == null)
      return 1.0;

    if (parentBlockState.getBlockInstance().equals(
        blockState.getBlockInstance()))
      return 1.0;

    // The block changed!
    System.out.println("blockFrom=" + parentBlockState.getBlockInstance());
    System.out.println("blockTo=" + blockState.getBlockInstance());

    return _probabilityOfBlockChange;
  }
}
