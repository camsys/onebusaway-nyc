package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

public class ZeroProbabilityParticleFilterException extends
    ParticleFilterException {

  private static final long serialVersionUID = 1L;

  public ZeroProbabilityParticleFilterException() {
    super("No particle has any likelihood!");
  }

}
