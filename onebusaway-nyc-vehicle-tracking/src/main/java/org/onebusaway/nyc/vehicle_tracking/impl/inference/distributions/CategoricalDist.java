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

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gov.sandia.cognition.statistics.DiscreteSamplingUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import org.apache.commons.math.util.MathUtils;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CategoricalDist<T extends Comparable<T>> {

  private static final boolean _sort = true;
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

  private final List<Integer> _objIdx = Lists.newArrayList();
  TObjectDoubleMap<T> _entriesToProbs;
  private Object[] _entries;

  DiscreteDistribution emd;
  private double[] _cummulativeDist;

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
    _entriesToProbs = new TObjectDoubleHashMap<T>();
  }

  public List<T> getSupport() {
    return new ArrayList<T>(_entriesToProbs.keySet());
  }

  public void put(double prob, T object) {

    Preconditions.checkNotNull(object);

    if (Double.compare(prob, 0.0) <= 0)
      return;

    _cumulativeProb += prob;
    final double newProb = _entriesToProbs.adjustOrPutValue(object, prob, prob);

    if (Double.compare(prob, newProb) == 0) {
      _objIdx.add(_objIdx.size());
    }

    /*
     * reset the underlying distribution for lazy reloading
     */
    emd = null;
  }

  @SuppressWarnings("unchecked")
  public T sample() {

    Preconditions.checkState(!_entriesToProbs.isEmpty(),
        "No entries in the CDF");
    Preconditions.checkState(_cumulativeProb > 0.0,
        "No cumulative probability in CDF");

    if (_entriesToProbs.size() == 1) {
      return Iterables.getOnlyElement(_entriesToProbs.keySet());
    }

    if (emd == null) {
      initializeDistribution();
    }
    

    final double u = threadLocalRng.get().nextDouble();
    final int newIdx = (int) emd.inverseF(u);

    return (T)_entries[newIdx];
  }

  private double[] initializeDistribution() {
    double[] probVector = MathUtils.normalizeArray(_entriesToProbs.values(),
        1.0);
    if (_sort) {
      probVector = handleSort(probVector);
    }
    /*
     * Need a double array for DiscreteDistribution. Lame.
     */
    final double[] cummulativeDist = new double[_objIdx.size()];
    final double[] doubleObjIdx = new double[_objIdx.size()];
    double prevVal = 0.0;
    for (int i = 0; i < _objIdx.size(); ++i) {
      cummulativeDist[i] = probVector[i] + prevVal;
      prevVal = cummulativeDist[i];
      doubleObjIdx[i] = _objIdx.get(i);
    }
    emd = new DiscreteDistribution(doubleObjIdx, probVector, _objIdx.size());
    _entries = _entriesToProbs.keys();
    return cummulativeDist;
  }
  
  /**
   * Sorts _objIdx and returns the reordered probabilities vector.
   * 
   */
  private double[] handleSort(double[] probs) {
    final Object[] mapKeys = _entriesToProbs.keys();
    /*
     * Sort the key index by key value, then reorder the prob value array with
     * sorted index.
     */
    Collections.sort(_objIdx, new Comparator<Integer>() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(Integer arg0, Integer arg1) {
        T p0 = (T)mapKeys[arg0];
        T p1 = (T)mapKeys[arg1];
        int probComp = Double.compare(_entriesToProbs.get(p0), _entriesToProbs.get(p1));
        if(probComp == 0)
          probComp = p0.compareTo(p1); 
        return probComp;
      }
    });
    final double[] newProbs = new double[probs.length];
    for (int i = 0; i < probs.length; ++i) {
      newProbs[i] = probs[_objIdx.get(i)];
    }
    return newProbs;
  }

  @SuppressWarnings("unchecked")
  public Multiset<T> sample(int samples) {
    
    Preconditions.checkArgument(samples > 0);
    Preconditions.checkState(!_entriesToProbs.isEmpty(),
        "No entries in the CDF");
    Preconditions.checkState(_cumulativeProb > 0.0,
        "No cumulative probability in CDF");

    final Multiset<T> sampled = HashMultiset.create(samples);
    if (_entriesToProbs.size() == 1) {
      sampled.add(Iterables.getOnlyElement(_entriesToProbs.keySet()));
    } else {

      if (emd == null) {
        _cummulativeDist = initializeDistribution();
      }
      
      int[] sampleIdxs = DiscreteSamplingUtil.sampleIndicesFromCumulativeProportions(
          threadLocalRng.get(), _cummulativeDist, samples);
      
      for (int i = 0; i < sampleIdxs.length; ++i) {
        sampled.add((T)_entries[_objIdx.get(sampleIdxs[i])]);
      }
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
