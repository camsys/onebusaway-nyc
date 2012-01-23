package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import java.util.Arrays;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.HalfNormalGen;
import umontreal.iro.lecuyer.randvar.InverseGammaGen;
import umontreal.iro.lecuyer.rng.RandomStream;

/**
 * The real working of this is through distance along the block (meters).
 * 
 * @author bwillard
 * 
 */
public class DistAlongDevDist implements
    ConjugateDist<ScheduleDevParams, Double, Double> {

  // TODO break these into separate classes
  private double[] _distAlongDevVarParams = {101.0, 90000.0};
  private Double _distAlongDevVarSample = null;

  private double[] _distAlongDevTransVarParams = {101.0, 360000.0};
  private Double _distAlongDevTransVarSample = null;

  private double[] _distAlongDevKalmanParams = {0.0, Math.pow(60.0 * 15.0, 2.0)};
  private Double _distAlongDevSample = null;
  private Double _lastDevSample = null;

  // private InverseGammaDist _scheduleVarDist = new
  // InverseGammaDist(_scheduleVarParams[0],
  // _scheduleVarParams[1]);

  RandomStream _rng;

  public DistAlongDevDist(DistAlongDevDist obj) {
    this._distAlongDevKalmanParams = obj._distAlongDevKalmanParams.clone();
    this._distAlongDevVarParams = obj._distAlongDevVarParams.clone();
    this._distAlongDevTransVarParams = obj._distAlongDevTransVarParams.clone();

    this._distAlongDevSample = obj._distAlongDevSample;
    this._distAlongDevVarSample = obj._distAlongDevVarSample;
    this._distAlongDevTransVarSample = obj._distAlongDevTransVarSample;

    this._lastDevSample = obj._lastDevSample;
    this._rng = obj._rng;
  }

  public DistAlongDevDist(RandomStream rnd) {
    _rng = rnd;
    Double[] priorSample = samplePrior();
    _distAlongDevVarSample = priorSample[0];
    _distAlongDevTransVarSample = priorSample[1];
    _distAlongDevSample = sample(null);
  }

  @Override
  public double getCurrentSample() {
    return _distAlongDevSample;
  }

  /**
   * This is the predictive density...
   */
  @Override
  public double density(Double schedDev, ScheduleDevParams condInput) {
    return NormalDist.density(
        _distAlongDevKalmanParams[0],
        Math.sqrt(_distAlongDevKalmanParams[1] + _distAlongDevTransVarSample
            + _distAlongDevVarSample), schedDev);
  }

  @Override
  public void updatePrior(Double schedDev, ScheduleDevParams condInput) {

    /*
     * update if it hasn't already been, since this value is necessary for
     * learning
     */
    if (_lastDevSample == _distAlongDevSample)
      _distAlongDevSample = HalfNormalGen.nextDouble(_rng, _distAlongDevSample,
          Math.sqrt(_distAlongDevTransVarSample));

    _distAlongDevTransVarParams[0] += 1.0;
    _distAlongDevTransVarParams[1] += Math.pow(schedDev - _distAlongDevSample,
        2.0);
    /*
     * predictive variance
     */
    double Q_t = _distAlongDevKalmanParams[1] + _distAlongDevTransVarSample
        + _distAlongDevVarSample;

    /*
     * Kalman gain "matrix"
     */
    double A_t = (_distAlongDevKalmanParams[1] + _distAlongDevTransVarSample)
        / Q_t;

    /*
     * posterior hyper-parameters
     */
    _distAlongDevKalmanParams[0] += A_t
        * (schedDev - _distAlongDevKalmanParams[0]);
    _distAlongDevKalmanParams[1] += _distAlongDevTransVarSample
        - Math.pow(A_t, 2.0) / Q_t;

    _distAlongDevVarParams[0] += 1.0;
    _distAlongDevVarParams[1] += Math.pow(_lastDevSample - _distAlongDevSample,
        2.0);

    /*
     * off-line-able suff. stat. propagation
     */
    Double[] priorSample = samplePrior();
    _distAlongDevVarSample = priorSample[0];
    _distAlongDevTransVarSample = priorSample[1];
  }

  @Override
  public Double[] samplePrior() {
    Double[] res = new Double[2];
    res[0] = InverseGammaGen.nextDouble(_rng, _distAlongDevVarParams[0] / 2.0,
        _distAlongDevVarParams[1] / 2.0);
    res[1] = InverseGammaGen.nextDouble(_rng,
        _distAlongDevTransVarParams[0] / 2.0,
        _distAlongDevTransVarParams[1] / 2.0);
    return res;
  }

  @Override
  public Double sample(ScheduleDevParams params) {
    _lastDevSample = _distAlongDevSample;
    _distAlongDevSample = HalfNormalGen.nextDouble(_rng,
        _distAlongDevKalmanParams[0], Math.sqrt(_distAlongDevKalmanParams[1]));
    return _distAlongDevSample;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("SchedulDevDist(");
    b.append("scheduleDevVarParams=").append(
        Arrays.toString(_distAlongDevVarParams)).append(",");
    b.append("scheduleDevMeanParams=").append(
        Arrays.toString(_distAlongDevKalmanParams)).append(",");
    b.append("scheduleDevTransVarParams=").append(
        Arrays.toString(_distAlongDevTransVarParams)).append(",");
    b.append("currentDevVarSample=").append(_distAlongDevVarSample).append(",");
    b.append("currentDevSample=").append(_distAlongDevSample).append(",");
    b.append("currentDevTransVarSample=").append(_distAlongDevTransVarSample);
    b.append(")");
    return b.toString();
  }
}
