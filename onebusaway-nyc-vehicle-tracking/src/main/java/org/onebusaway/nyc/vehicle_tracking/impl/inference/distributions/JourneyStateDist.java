package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.realtime.api.EVehiclePhase;

import org.apache.commons.lang.ArrayUtils;

import umontreal.iro.lecuyer.randvar.BinomialGen;
import umontreal.iro.lecuyer.randvarmulti.DirichletGen;
import umontreal.iro.lecuyer.rng.RandomStream;

/**
 * This class defines a distribution over the possible journey states TODO this
 * is really beta-binomial
 * 
 * @author bwillard
 * 
 */
public class JourneyStateDist implements
    ConjugateDist<JourneyStateParams, EVehiclePhase, Double> {

  /*
   * 0: in-progress probability 1: not-in-progress probability
   */
  private final double[] _dscTypePriors = {10 * 0.75, 10 * 0.25};
  private double[] _currentDscTypeProbs = new double[2];

  private final RandomStream _rng;

  public JourneyStateDist(RandomStream rng) {
    _rng = rng;
  }

  @Override
  public EVehiclePhase sample(JourneyStateParams condParams) {

    final Double[] stateProbs = samplePrior();

    final int inProgress = BinomialGen.nextInt(_rng, 1, stateProbs[0]);
    // At this point, we could be dead heading before a block or actually on a
    // block in progress. We slightly favor blocks already in progress
    if (inProgress > 0) {
      return EVehiclePhase.IN_PROGRESS;
    } else {
      return EVehiclePhase.DEADHEAD_BEFORE;
    }
  }

  private int getIndexForObs(EVehiclePhase phase) {
    if (phase == EVehiclePhase.IN_PROGRESS) {
      return 0;
    } else if (phase == EVehiclePhase.DEADHEAD_BEFORE) {
      return 1;
    } else {
      return -1;
    }
  }

  @Override
  public double density(EVehiclePhase phase, JourneyStateParams condParams) {
    return _currentDscTypeProbs[getIndexForObs(phase)];
  }

  @Override
  public void updatePrior(EVehiclePhase phase, JourneyStateParams condParams) {
    _dscTypePriors[getIndexForObs(phase)]++;

  }

  @Override
  public Double[] samplePrior() {
    final double[] sampleProbs = new double[2];
    DirichletGen.nextPoint(_rng, _dscTypePriors, sampleProbs);
    _currentDscTypeProbs = sampleProbs;
    return ArrayUtils.toObject(sampleProbs);
  }

  @Override
  public double getCurrentSample() {
    // TODO Auto-generated method stub
    return 0;
  }

}
