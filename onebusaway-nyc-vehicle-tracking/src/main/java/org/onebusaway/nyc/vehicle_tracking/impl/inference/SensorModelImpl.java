package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<NycVehicleLocationRecord> {

  private Gaussian _locationDeviationModel = new Gaussian(0.0, 1.0);

  @Override
  public double likelihood(Particle particle, NycVehicleLocationRecord obs) {

    VehicleState state = particle.getData();

    // Really simple starter sensor model: penalize for distance away from
    // observed GPS location
    double locationDeviation = SphericalGeometryLibrary.distance(
        obs.getLatitude(), obs.getLongitude(), state.getLat(), state.getLon());
    return _locationDeviationModel.getProbability(locationDeviation);
  }
}
