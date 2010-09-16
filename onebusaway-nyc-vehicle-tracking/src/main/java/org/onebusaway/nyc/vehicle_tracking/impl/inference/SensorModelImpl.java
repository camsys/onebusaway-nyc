package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.realtime.BlockInstance;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocation;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private Gaussian _locationDeviationModel = new Gaussian(0.0, 20);

  private Gaussian _tripLocationDeviationModel = new Gaussian(0.0, 20);

  private Gaussian _scheduleDeviationModel = new Gaussian(0.0, 15 * 60);

  /**
   * 
   * @param locationSigma in meters
   */
  public void setLocationSigma(double locationSigma) {
    _locationDeviationModel = new Gaussian(0, locationSigma);
  }

  /**
   * 
   * @param tripLocationSigma in meters
   */
  public void setTripLocationSigma(double tripLocationSigma) {
    _tripLocationDeviationModel = new Gaussian(0, tripLocationSigma);
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  public void setScheduleDeviationSigma(double scheduleDeviationSigma) {
    _scheduleDeviationModel = new Gaussian(0, scheduleDeviationSigma * 60);
  }

  @Override
  public double likelihood(Particle particle, Observation obs) {

    // Really simple starter sensor model: penalize for distance away from
    // observed GPS location
    double pLocation = computeLocationVsGpsPropability(particle, obs);

    // Penalize for distance away from trip path
    double pTrip = computeLocationVsTripProbability(particle);
    
    // Penalize for deviation from the schedule
    double pSchedule = computeScheduleDeviationProbability(particle, obs);

    // log-likelihood is more mathematically stable
    return pLocation * pTrip * pSchedule;
  }

  private double computeLocationVsGpsPropability(Particle particle,
      Observation obs) {

    VehicleState state = particle.getData();
    ProjectedPoint point = obs.getPoint();

    double locationDeviation = state.getEdgeState().getPointOnEdge().distance(
        point);
    return _locationDeviationModel.getProbability(locationDeviation);
  }

  private double computeLocationVsTripProbability(Particle particle) {

    VehicleState state = particle.getData();
    BlockState blockState = state.getBlockState();

    if (blockState == null)
      return 1.0;

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = state.getEdgeState().getPointOnEdge();

    double distance = SphericalGeometryLibrary.distance(p1.getLat(),
        p2.getLon(), p2.getLat(), p2.getLon());
    return _tripLocationDeviationModel.getProbability(distance);
  }

  private double computeScheduleDeviationProbability(Particle particle,
      Observation observation) {

    VehicleState state = particle.getData();
    BlockState blockState = state.getBlockState();

    if (blockState == null)
      return 1.0;

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    long scheduledTime = blockInstance.getServiceDate()
        + blockLocation.getScheduledTime() * 1000;

    NycVehicleLocationRecord record = observation.getRecord();

    long delta = Math.abs(scheduledTime - record.getTime()) / 1000;

    return _scheduleDeviationModel.getProbability(delta);
  }
}
