package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import java.util.Set;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import umontreal.iro.lecuyer.rng.RandomStream;

public class RunTransitionDist extends DirichletConjTransitionDist<RunTripEntry> {

  private static final double _initialPriorOffDiagVal = 1.0;
  private static final double _initialPriorDiagVal = 90.0;
  private static final double _initialPriorNextTripVal = 80.0;

  public RunTransitionDist(RunTransitionDist obj) {
    super(obj);
  }
  public RunTransitionDist(RandomStream rng) {
    super(rng);
  }

  public double density(RunTripEntry runTo, RunTripEntry runFrom) {
    return this.density(runTo, new TransDistParams<RunTripEntry>(null, null, runFrom, null));
  }

  @Override
  synchronized protected void addEntries(Set<RunTripEntry> missingEntries,
      TransDistParams<RunTripEntry> condParams) {
    for (RunTripEntry rte : missingEntries) {
      Double prob = 0.0;
      if (rte.equals(condParams.getCurrentState())) {
        prob = _initialPriorDiagVal;
      } else if(rte.equals(condParams.getNextState())) {
        prob = _initialPriorNextTripVal;
      } else {
        prob = _initialPriorOffDiagVal;
      }
      _transitionPriors.put(condParams.getCurrentState(), rte, prob);
    }
  }

  /**
   * this would be a lot to return, potentially.
   * most importantly, we don't need it.
   */
  @Override
  public double getCurrentSample() {
    return Double.NaN;
  }
}
