package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private Gaussian _locationDeviationModel = new Gaussian(0.0, 20);

  /**
   * 
   * @param locationVariance in meters
   */
  public void setLocationVariance(double locationVariance) {
    _locationDeviationModel = new Gaussian(0, locationVariance);
  }

  @Override
  public double likelihood(Particle particle, Observation obs) {

    VehicleState state = particle.getData();
    ProjectedPoint point = obs.getPoint();

    // Really simple starter sensor model: penalize for distance away from
    // observed GPS location
    double locationDeviation = state.getEdgeState().getPointOnEdge().distance(
        point);
    return _locationDeviationModel.getProbability(locationDeviation);
  }
}
