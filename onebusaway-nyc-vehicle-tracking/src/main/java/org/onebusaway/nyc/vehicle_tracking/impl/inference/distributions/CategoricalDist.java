/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.apache.commons.math.util.MathUtils;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;

public class CategoricalDist<T> {

  private double _cumulativeProb = 0.0;

  static class LocalRandom extends ThreadLocal<Random> {
    long _seed = 0;

    LocalRandom(long seed) {
      _seed = seed;
    }

    @Override
    protected Random initialValue() {
      if (_seed != 0)
        return new Random(_seed);
      else
        return new Random();
    }
  }

  static class LocalRandomDummy extends ThreadLocal<Random> {
    private static Random rng;

    LocalRandomDummy(long seed) {
      if (seed != 0)
        rng = new Random(seed);
      else
        rng = new Random();
    }

    @Override
    synchronized public Random get() {
      return rng;
    }
  }

  static ThreadLocal<Random> threadLocalRng;
  static {
    if (!ParticleFilter.getTestingEnabled()) {
      threadLocalRng = new LocalRandom(0);
    } else {
      threadLocalRng = new LocalRandomDummy(0);

    }
  }

  private List<Double> _objIdx = new ArrayList<Double>();
  TreeMap<T, Double> _entriesToProbs;
  private List<T> _entries;

  DiscreteDistribution emd;

  public static ThreadLocal<Random> getThreadLocalRng() {
    return threadLocalRng;
  }

  synchronized public static void setSeed(long seed) {
    if (!ParticleFilter.getTestingEnabled()) {
      threadLocalRng = new LocalRandom(seed);
    } else {
      threadLocalRng = new LocalRandomDummy(seed);
    }
  }

  public CategoricalDist() {
    // _entriesToProbs = new TreeMap<T, Double>(Ordering.usingToString());
    _entriesToProbs = new TreeMap<T, Double>();
  }

  public CategoricalDist(Comparator<T> order) {
    // _entriesToProbs = new TreeMap<T, Double>(Ordering.usingToString());
    _entriesToProbs = new TreeMap<T, Double>(order);
  }

  public List<T> getSupport() {
    return new ArrayList<T>(_entriesToProbs.keySet());
  }

  public void put(double prob, T object) {
    
    Preconditions.checkNotNull(object);
    
    if (Double.compare(prob, 0.0) <= 0)
      return;

    _cumulativeProb += prob;
    Double currentProb = _entriesToProbs.get(object);

    if (currentProb == null) {
      _entriesToProbs.put(object, prob);
      _objIdx.add((double) _objIdx.size());
    } else {
      /*
       * allow duplicate entries' probability to compound
       */
      _entriesToProbs.put(object, prob + currentProb);
    }

    /*
     * reset the underlying distribution for lazy reloading
     */
    emd = null;

  }

  public T sample() {

    if (_entriesToProbs.isEmpty())
      throw new IllegalStateException("No entries in the CDF");

    if (_cumulativeProb == 0.0)
      throw new IllegalStateException("No cumulative probability in CDF");

    if (_entriesToProbs.size() == 1) {
      return _entriesToProbs.keySet().iterator().next();
      // return _entriesToProbs.firstKey();
    }

    if (emd == null) {
      double[] probs = MathUtils.normalizeArray(
          Doubles.toArray(_entriesToProbs.values()), 1.0);
      emd = new DiscreteDistribution(Doubles.toArray(_objIdx), probs,
          _objIdx.size());
      _entries = getSupport();
    }

    double u = threadLocalRng.get().nextDouble();
    int newIdx = (int) emd.inverseF(u);

    return _entries.get(newIdx);
  }

  public Multiset<T> sample(int samples) {

    Preconditions.checkState(!_entriesToProbs.isEmpty());
    Preconditions.checkState(_cumulativeProb > 0.0);
    Preconditions.checkArgument(samples > 0);

    Multiset<T> sampled = HashMultiset.create(samples);

    for (int i = 0; i < samples; ++i) {
      sampled.add(sample());
    }

    return sampled;
  }

  public boolean isEmpty() {
    return _entriesToProbs.isEmpty();
  }

  public boolean hasProbability() {
    return _cumulativeProb > 0.0;
  }

  public boolean canSample() {
    return !_entriesToProbs.isEmpty() && _cumulativeProb > 0.0;
  }

  public int size() {
    return _entriesToProbs.size();
  }

  @Override
  public String toString() {
    return _entriesToProbs.toString();
  }

  public double density(T thisState) {
    return _entriesToProbs.get(thisState);
  }
}
