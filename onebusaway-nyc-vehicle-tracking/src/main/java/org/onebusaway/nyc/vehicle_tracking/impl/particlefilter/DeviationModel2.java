package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;

public class DeviationModel2 implements ProbabilityFunction {

  private Normal _normal;

  private double _sigma;

  private double _offset;

  public DeviationModel2(double sigma) {
    this(sigma, 2.0);
  }

  public DeviationModel2(double sigma, double offset) {
    _normal = new Normal(0, 1.0, RandomEngine.makeDefault());
    _sigma = sigma;
    _offset = offset;
  }

  public double probability(double deviation) {
    return _normal.cdf(_offset * (1 - deviation / _sigma));
  }
}
