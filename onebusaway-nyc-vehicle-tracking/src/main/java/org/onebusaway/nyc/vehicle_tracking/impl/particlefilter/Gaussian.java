package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.Random;

/**
 * A normal distribution: just a mean and a sigma. Think bell curve.
 * 
 * @author bdferris
 */
public class Gaussian implements Comparable<Gaussian> {

  private static final long serialVersionUID = 1L;

  private static final double SQRT_TWO = Math.sqrt(2.0);

  private static Random _random = new Random();

  private final double _mean;

  private final double _sigma;

  public Gaussian(Gaussian g) {
    this(g.getMean(), g.getSigma());
  }

  public Gaussian(double mean, double sigma) {
    _mean = mean;
    _sigma = sigma;

    if (_sigma < 0)
      throw new IllegalArgumentException(
          "Sigma must be greater than or equal to zero");
  }

  public double getMean() {
    return _mean;
  }

  public double getSigma() {
    return _sigma;
  }

  public double getHeightAt(double p) {
    double distance = _mean - p;
    double scalar = 1.0 / (_sigma * Math.sqrt(2.0 * Math.PI));
    double exponent = -1.0 * ((distance * distance) / (2.0 * _sigma * _sigma));
    return scalar * Math.exp(exponent);
  }

  public double getCDF(double x) {
    double arg = (x - _mean) / (_sigma * SQRT_TWO);
    return 0.5 * (1 + erf(arg));
  }

  public Gaussian multiplyBy(Gaussian other) {
    double sigma = (1.0 / ((1.0 / _sigma) + (1.0 / other.getSigma())));
    double mean = sigma
        * (((1.0 / _sigma) * _mean) + ((1.0 / other.getSigma()) * other.getMean()));
    return new Gaussian(mean, sigma);
  }

  public double likelihoodIsSame(Gaussian g) {
    Gaussian mix = new Gaussian(getMean(), getSigma() + g.getSigma());
    return mix.getHeightAt(g.getMean());
  }

  public double getProbability(double value) {
    return getHeightAt(value);
  }

  public double drawSample() {
    return _random.nextGaussian() * _sigma + _mean;
  }

  /***************************************************************************
   * {@link Comparable} Interface
   **************************************************************************/

  public int compareTo(Gaussian o) {
    if (_mean != o._mean)
      return _mean < o._mean ? -1 : 1;
    if (_sigma != o._sigma)
      return _sigma < o._sigma ? -1 : 1;
    return 0;
  }

  /***************************************************************************
   * {@link Object} Interface
   **************************************************************************/

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_mean);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_sigma);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Gaussian other = (Gaussian) obj;
    if (Double.doubleToLongBits(_mean) != Double.doubleToLongBits(other._mean))
      return false;
    if (Double.doubleToLongBits(_sigma) != Double.doubleToLongBits(other._sigma))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "mean: " + _mean + " sigma: " + _sigma;
  }

  /***************************************************************************
   * Private Methods
   **************************************************************************/

  // fractional error in math formula less than 1.2 * 10 ^ -7.
  // although subject to catastrophic cancellation when z in very close to 0
  private double erf(double z) {
    double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

    // use Horner's method
    double ans = 1
        - t
        * Math.exp(-z
            * z
            - 1.26551223
            + t
            * (1.00002368 + t
                * (0.37409196 + t
                    * (0.09678418 + t
                        * (-0.18628806 + t
                            * (0.27886807 + t
                                * (-1.13520398 + t
                                    * (1.48851587 + t
                                        * (-0.82215223 + t * (0.17087277))))))))));
    if (z >= 0)
      return ans;
    else
      return -ans;
  }
}
