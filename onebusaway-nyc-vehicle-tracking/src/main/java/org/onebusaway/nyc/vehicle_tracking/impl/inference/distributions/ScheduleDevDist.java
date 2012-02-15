package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.InverseGammaGen;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.rng.RandomStream;

import java.util.Arrays;

/**
 * The real working of this is through distance along the block (meters).
 * 
 * @author bwillard
 * 
 */
public class ScheduleDevDist implements
    ConjugateDist<ScheduleDevParams, Double, Double> {

  // TODO break these into separate classes
  private double[] _scheduleDevVarParams = {101.0, 90000.0};
  private Double _currentDevVarSample = null;

  private double[] _scheduleDevTransVarParams = {101.0, 360000.0};
  private Double _currentDevTransVarSample = null;

  private double[] _scheduleDevKalmanParams = {0.0, Math.pow(60.0 * 15.0, 2.0)};
  private Double _currentDevSample = null;
  private Double _lastDevSample = null;

  // private InverseGammaDist _scheduleVarDist = new
  // InverseGammaDist(_scheduleVarParams[0],
  // _scheduleVarParams[1]);

  RandomStream _rng;

  public ScheduleDevDist(ScheduleDevDist obj) {
    this._scheduleDevKalmanParams = obj._scheduleDevKalmanParams.clone();
    this._scheduleDevVarParams = obj._scheduleDevVarParams.clone();
    this._scheduleDevTransVarParams = obj._scheduleDevTransVarParams.clone();

    this._currentDevSample = obj._currentDevSample;
    this._currentDevVarSample = obj._currentDevVarSample;
    this._currentDevTransVarSample = obj._currentDevTransVarSample;

    this._lastDevSample = obj._lastDevSample;
    this._rng = obj._rng;
  }

  public ScheduleDevDist(RandomStream rnd) {
    _rng = rnd;
    final Double[] priorSample = samplePrior();
    _currentDevVarSample = priorSample[0];
    _currentDevTransVarSample = priorSample[1];
    _currentDevSample = sample(null);
  }

  @Override
  public double getCurrentSample() {
    return _currentDevSample;
  }

  /**
   * This is the predictive density...
   */
  @Override
  public double density(Double schedDev, ScheduleDevParams condInput) {
    return NormalDist.density(
        _scheduleDevKalmanParams[0],
        Math.sqrt(_scheduleDevKalmanParams[1] + _currentDevTransVarSample
            + _currentDevVarSample), schedDev);
  }

  @Override
  public void updatePrior(Double schedDev, ScheduleDevParams condInput) {

    /*
     * update if it hasn't already been, since this value is necessary for
     * learning
     */
    if (_lastDevSample.equals(_currentDevSample))
      _currentDevSample = NormalGen.nextDouble(_rng, _currentDevSample,
          Math.sqrt(_currentDevTransVarSample));

    _scheduleDevTransVarParams[0] += 1.0;
    _scheduleDevTransVarParams[1] += Math.pow(schedDev - _currentDevSample, 2.0);
    /*
     * predictive variance
     */
    final double Q_t = _scheduleDevKalmanParams[1] + _currentDevTransVarSample
        + _currentDevVarSample;

    /*
     * Kalman gain "matrix"
     */
    final double A_t = (_scheduleDevKalmanParams[1] + _currentDevTransVarSample)
        / Q_t;

    /*
     * posterior hyper-parameters
     */
    _scheduleDevKalmanParams[0] += A_t
        * (schedDev - _scheduleDevKalmanParams[0]);
    _scheduleDevKalmanParams[1] += _currentDevTransVarSample
        - Math.pow(A_t, 2.0) / Q_t;

    _scheduleDevVarParams[0] += 1.0;
    _scheduleDevVarParams[1] += Math.pow(_lastDevSample - _currentDevSample,
        2.0);

    /*
     * off-line-able suff. stat. propagation
     */
    final Double[] priorSample = samplePrior();
    _currentDevVarSample = priorSample[0];
    _currentDevTransVarSample = priorSample[1];
  }

  @Override
  public Double[] samplePrior() {
    final Double[] res = new Double[2];
    res[0] = InverseGammaGen.nextDouble(_rng, _scheduleDevVarParams[0] / 2.0,
        _scheduleDevVarParams[1] / 2.0);
    res[1] = InverseGammaGen.nextDouble(_rng,
        _scheduleDevTransVarParams[0] / 2.0,
        _scheduleDevTransVarParams[1] / 2.0);
    return res;
  }

  @Override
  public Double sample(ScheduleDevParams params) {
    _lastDevSample = _currentDevSample;
    _currentDevSample = NormalGen.nextDouble(_rng, _scheduleDevKalmanParams[0],
        Math.sqrt(_scheduleDevKalmanParams[1]));
    return _currentDevSample;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("SchedulDevDist(");
    b.append("scheduleDevVarParams=").append(
        Arrays.toString(_scheduleDevVarParams)).append(",");
    b.append("scheduleDevMeanParams=").append(
        Arrays.toString(_scheduleDevKalmanParams)).append(",");
    b.append("scheduleDevTransVarParams=").append(
        Arrays.toString(_scheduleDevTransVarParams)).append(",");
    b.append("currentDevVarSample=").append(_currentDevVarSample).append(",");
    b.append("currentDevSample=").append(_currentDevSample).append(",");
    b.append("currentDevTransVarSample=").append(_currentDevTransVarSample);
    b.append(")");
    return b.toString();
  }
}
