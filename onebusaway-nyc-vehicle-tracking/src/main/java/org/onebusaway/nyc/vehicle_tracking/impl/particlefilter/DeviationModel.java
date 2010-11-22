package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

public class DeviationModel implements ProbabilityFunction{

  private final HalfNormal _dist;

  public DeviationModel(double sigma) {
    _dist = new HalfNormal(sigma);
  }

  public double probability(double deviation) {
    return 1 - _dist.cdf(deviation);
  }
}
