package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

public interface ProbabilityFunction {
  public double probability(double value);
}
