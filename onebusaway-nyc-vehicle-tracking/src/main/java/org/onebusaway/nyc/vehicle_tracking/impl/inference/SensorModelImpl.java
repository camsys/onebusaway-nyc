package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceRecord;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements
    SensorModel<VehicleLocationInferenceRecord> {

  private Gaussian _locationDeviationModel = new Gaussian(0.0, 1.0);

  @Override
  public double likelihood(Particle particle, VehicleLocationInferenceRecord obs) {

    VehicleState state = particle.getData();

    // Really simple starter sensor model: penalize for distance away from
    // observed GPS location
    double locationDeviation = SphericalGeometryLibrary.distance(obs.getLat(),
        obs.getLon(), state.getLat(), state.getLon());
    return _locationDeviationModel.getProbability(locationDeviation);
  }
}
