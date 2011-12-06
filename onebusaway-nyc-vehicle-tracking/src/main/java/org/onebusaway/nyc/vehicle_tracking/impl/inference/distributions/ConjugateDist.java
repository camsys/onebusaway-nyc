package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;


/**
 * This interface is to be implemented by distributions
 * which have a defined conjugate and update procedure. 
 * @author bwillard
 *
 */
public interface ConjugateDist<Args, Support, PriorSupport> {

  public double density(Support s, Args obs);
  public void updatePrior(Support s, Args obs);
  PriorSupport[] samplePrior();
  Support sample(Args obs);
  double getCurrentSample();
}
