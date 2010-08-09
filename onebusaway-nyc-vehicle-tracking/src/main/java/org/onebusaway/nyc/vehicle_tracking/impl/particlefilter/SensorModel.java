package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

/**
 * Sensor model strategy for evaluating the likelihood of a particle given an
 * observation.
 * 
 * @author bdferris
 * @see ParticleFilter
 */
public interface SensorModel<OBS> {

  /**
   * @return likelihood probability on the interval [0.0,1.0]
   */
  public double likelihood(Particle particle, OBS observation);
}
