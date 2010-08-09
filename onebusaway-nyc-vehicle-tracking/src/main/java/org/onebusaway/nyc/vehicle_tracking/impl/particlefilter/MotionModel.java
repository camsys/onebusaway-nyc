package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

/**
 * Particle motion model interface for defining the strategy for moving
 * particles.
 * 
 * @author bdferris
 * 
 * @param <OBS>
 */
public interface MotionModel<OBS> {

  /**
   * @param parent the parent of the new particle
   * @param timestamp timestamp of the new particle
   * @param timeElapsed time elapsed since last move
   * @param obs observation at the given timestamp
   * @return a newly moved particle
   * @throws Exception TODO
   */
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      OBS obs);
}
