package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.List;

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
   * @param results TODO
   * @throws Exception TODO
   */
  public void move(Particle parent, double timestamp, double timeElapsed,
      OBS obs, List<Particle> results);
}
