package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.realtime.api.EVehiclePhase;

import umontreal.iro.lecuyer.probdistmulti.DirichletDist;
import umontreal.iro.lecuyer.randvar.BinomialGen;
import umontreal.iro.lecuyer.randvarmulti.DirichletGen;
import umontreal.iro.lecuyer.rng.RandomStream;

/**
 * This class defines a distribution over the possible journey states
 * TODO this is really beta-binomial
 * 
 * @author bwillard
 * 
 */
public class JourneyStateDist implements
    ConjugateDist<JourneyStateParams, EVehiclePhase, Double> {

  /*
   * 0: in-progress probability
   * 1: not-in-progress probability
   */
  private double[] _dscTypePriors = { 10 * 0.75, 10 * 0.25 };
  private double[] _currentDscTypeProbs = new double[2];

  private RandomStream _rng;
  
  public JourneyStateDist(RandomStream rng) {
    _rng = rng;
  }

  @Override
  public EVehiclePhase sample(JourneyStateParams condParams) {
    
    Double[] stateProbs = samplePrior();

    int inProgress = BinomialGen.nextInt(_rng, 1, stateProbs[0]);
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
    double[] sampleProbs = new double[2];
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
