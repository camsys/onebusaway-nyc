package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.List;

/**
 * Factory interface for creating an initial set of particles from an initial
 * observation
 * 
 * @param <OBS> the observation type
 * @author bdferris
 * 
 * @see ParticleFilter
 */
public interface ParticleFactory<OBS> {

  /**
   * @param timestamp time of the initial observation
   * @param observation the initial observation
   * @return the initial list of particles
   */
  public List<Particle> createParticles(double timestamp, OBS observation);
}
