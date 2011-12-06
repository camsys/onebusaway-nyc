package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import java.util.Arrays;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

import umontreal.iro.lecuyer.probdist.HalfNormalDist;
import umontreal.iro.lecuyer.probdist.InverseGammaDist;
import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.HalfNormalGen;
import umontreal.iro.lecuyer.randvar.InverseGammaGen;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.rng.RandomStream;

public class LocationDevDist implements ConjugateDist<LocationDevParams, Double, Double> {

  /*
   * variance centered around 400m (~+-10m in 98% interval)
   */
  private double[] _locationVarParams = { 101.0, 2700.0 };
  private Double _currentLocVarSample = null;

//  private InverseGammaDist _locationVarDist = new InverseGammaDist(
//      _locationVarParams[0], _locationVarParams[1]);

  private RandomStream _rng;
  private Double _lastLocVarSample = null;

  public LocationDevDist(LocationDevDist obj) {
    this._locationVarParams = obj._locationVarParams.clone();
    this._currentLocVarSample = obj._currentLocVarSample;
    this._lastLocVarSample = obj._lastLocVarSample;
    this._rng = obj._rng;
  }
  
  public LocationDevDist(RandomStream rng) {
    _rng = rng;
    _currentLocVarSample = samplePrior()[0];
  }

  @Override
  public double density(Double locDev, LocationDevParams condParams) {
    return HalfNormalDist.density(0.0, Math.sqrt(_currentLocVarSample), locDev);
  }

  @Override
  public void updatePrior(Double locDev, LocationDevParams condParams) {
    _locationVarParams[0] += 1.0;
    _locationVarParams[1] += Math.pow(locDev, 2.0);

    _currentLocVarSample = samplePrior()[0];
  }

  @Override
  public Double[] samplePrior() {
    Double[] ret = new Double[1];
    ret[0] = InverseGammaGen.nextDouble(_rng,
        _locationVarParams[0] / 2.0, _locationVarParams[1] / 2.0);
    return ret;
  }

  @Override
  public Double sample(LocationDevParams obs) {
    _lastLocVarSample = _currentLocVarSample;
    _currentLocVarSample = HalfNormalGen.nextDouble(_rng, 0.0, Math.sqrt(_currentLocVarSample));
    return _currentLocVarSample;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("LocationDevDist(");
    b.append("locationVarParams=").append(Arrays.toString(_locationVarParams))
    .append(",");
    b.append("currentLocVarSample=").append(_currentLocVarSample).append(",");
    b.append(")");
    return b.toString();
  }

  @Override
  public double getCurrentSample() {
    return _currentLocVarSample;
  }

}
