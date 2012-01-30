package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math.util.MathUtils;

import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;
import com.google.common.primitives.Doubles;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;
import umontreal.iro.lecuyer.randvarmulti.DirichletGen;
import umontreal.iro.lecuyer.rng.RandomStream;

/**
 * This is a generic, sparse, conjugate Dirichlet distributed transition
 * probability matrix.
 * 
 * @author bwillard
 * 
 */
public abstract class DirichletConjTransitionDist<SupportType> implements
    ConjugateDist<TransDistParams<SupportType>, SupportType, Double> {

  /*
   * ordered tables and maps are used in an attempt to ensure ordering of the
   * support, since it is possible to have duplicate probabilities for different
   * elements of the support, and we want to insure reproducibility over seeds
   * in the random number generator.
   */
  TreeBasedTable<SupportType, SupportType, Double> _transitionPriors = TreeBasedTable.create(
      Ordering.usingToString(), Ordering.usingToString());

  TreeBasedTable<SupportType, SupportType, Double> _currentTransProbs = TreeBasedTable.create(
      Ordering.usingToString(), Ordering.usingToString());

  private RandomStream _rng;

  public DirichletConjTransitionDist(
      DirichletConjTransitionDist<SupportType> obj) {
    this._currentTransProbs = obj._currentTransProbs;
    this._transitionPriors = obj._transitionPriors;
    this._rng = obj._rng;
  }

  public DirichletConjTransitionDist(RandomStream rng) {
    _rng = rng;
  }

  /**
   * This method is implemented by the user, so as to allow more elaborate
   * initial values.
   * 
   * @param missingEntries
   * @param condParams
   */
  protected abstract void addEntries(Set<SupportType> missingEntries,
      TransDistParams<SupportType> condParams);

  /**
   * Returns the probabilities for a form -> to transition, where "to" is only a
   * subset of the possible transitions.
   * 
   * @param condParams
   * @return
   */
  private SortedMap<SupportType, Double> getProbsOverSubset(
      TransDistParams<SupportType> condParams) {

    Map<SupportType, Double> currRow = _currentTransProbs.row(condParams.getCurrentState());
    Set<SupportType> thisSupport = condParams.getSupport();
    Set<SupportType> missingEntries = Sets.difference(thisSupport,
        currRow.keySet());
    /*
     * if some entries don't exist in the table, initialize them
     */
    addEntries(missingEntries, condParams);
    updateTransProbs(condParams);
    /*
     * now, just return a map of the support TODO use Maps.filterKeys?
     */
    SortedMap<SupportType, Double> retMap = new TreeMap<SupportType, Double>();
    for (SupportType rte : thisSupport) {
      retMap.put(rte, _currentTransProbs.get(condParams.getCurrentState(), rte));
    }
    return retMap;
  }

  synchronized protected void updateTransProbs(
      TransDistParams<SupportType> condParams) {
    Map<SupportType, Double> newPriors = samplePrior(condParams);
    /*
     * update our current transition probabilities
     */
    for (Entry<SupportType, Double> entry : newPriors.entrySet()) {
      _currentTransProbs.put(condParams.getCurrentState(), entry.getKey(),
          entry.getValue());
    }
  }

  /**
   * Get the density of Arg2.currentState -> Arg1
   */
  @Override
  public double density(SupportType obsRunTrip,
      TransDistParams<SupportType> condParams) {
    Double prob = _currentTransProbs.get(condParams.getCurrentState(),
        obsRunTrip);
    if (prob == null) {
      Set<SupportType> missingEntries = new HashSet<SupportType>();

      missingEntries.add(obsRunTrip);
      missingEntries.add(condParams.getCurrentState());

      addEntries(missingEntries, condParams);
      updateTransProbs(condParams);

      prob = _currentTransProbs.get(condParams.getCurrentState(), obsRunTrip);
    }
    return prob;
  }

  @Override
  synchronized public void updatePrior(SupportType obsRunTrip,
      TransDistParams<SupportType> condParams) {
    Double prob = _transitionPriors.get(condParams.getCurrentState(),
        obsRunTrip);
    if (prob == null) {
      Set<SupportType> missingEntries = new HashSet<SupportType>();
      missingEntries.add(obsRunTrip);
      missingEntries.add(condParams.getCurrentState());
      addEntries(missingEntries, condParams);
      prob = _transitionPriors.get(condParams.getCurrentState(), obsRunTrip);
    }
    /*
     * increase prior for transfer from current run-trip to observed run-trip
     */
    prob++;
    _transitionPriors.put(condParams.getCurrentState(), obsRunTrip, prob);
    updateTransProbs(condParams);
  }

  /*
   * we only need to resample per row (e.g. from->to probs). TODO check this
   */
  public TreeMap<SupportType, Double> samplePrior(
      TransDistParams<SupportType> condParams) {
    Map<SupportType, Double> currPriorsForSupport = _transitionPriors.row(condParams.getCurrentState());
    double[] sampleProbs = new double[currPriorsForSupport.size()];
    DirichletGen.nextPoint(_rng,
        Doubles.toArray(currPriorsForSupport.values()), sampleProbs);
    TreeMap<SupportType, Double> retMap = new TreeMap<SupportType, Double>(
        Ordering.usingToString());
    int i = 0;
    for (SupportType rte : currPriorsForSupport.keySet()) {
      retMap.put(rte, sampleProbs[i]);
      ++i;
    }
    return retMap;
  }

  @Override
  public Double[] samplePrior() {
    throw new NotImplementedException("must call method with parameters");
  }

  @SuppressWarnings("unchecked")
  @Override
  public SupportType sample(TransDistParams<SupportType> condParams) {
    Map<SupportType, Double> currProbsForSupport = getProbsOverSubset(condParams);
    double[] probs = MathUtils.normalizeArray(
        Doubles.toArray(currProbsForSupport.values()), 1.0);
    double[] objIdx = new double[currProbsForSupport.size()];
    // TODO ugh. do something better
    for (int i = 0; i < currProbsForSupport.size(); ++i)
      objIdx[i] = i;
    DiscreteDistribution emd = new DiscreteDistribution(objIdx, probs,
        objIdx.length);

    double u = _rng.nextDouble();
    int newIdx = (int) emd.inverseF(u);

    // TODO another ugh.
    return (SupportType) condParams.getSupport().toArray()[newIdx];
  }

}
