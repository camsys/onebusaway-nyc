package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

/**
 * Penalize a particle when its on-street location deviates substantially from
 * the observed GPS location.
 * 
 * @author bdferris
 */
public class StreetLocationSensorModelImpl implements SensorModel<Observation> {

  private DeviationModel _locationDeviationModel = new DeviationModel(20);

  /**
   * 
   * @param locationSigma in meters
   */
  public void setLocationSigma(double locationSigma) {
    _locationDeviationModel = new DeviationModel(locationSigma);
  }

  @Override
  public double likelihood(Particle particle, Observation obs) {

    VehicleState state = particle.getData();
    ProjectedPoint point = obs.getPoint();

    double locationDeviation = state.getEdgeState().getPointOnEdge().distance(
        point);

    // Normalize probability to 1
    return _locationDeviationModel.probability(locationDeviation);
  }
}
