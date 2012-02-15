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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import org.apache.commons.math.util.MathUtils;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class CategoricalDist<T extends Comparable<T>> {

  private static final boolean _sort = true;
  private double _cumulativeProb = 0.0;
  private double[] probCache;

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
    double newProb = _entriesToProbs.adjustOrPutValue(object, prob, prob);

    if (Double.compare(prob, newProb) == 0) {
      _objIdx.add(_objIdx.size());
    } 

    /*
     * reset the underlying distribution for lazy reloading
     */
    emd = null;
  }

  public T sample() {

    Preconditions.checkState(!_entriesToProbs.isEmpty(), "No entries in the CDF");
    Preconditions.checkState(_cumulativeProb > 0.0, "No cumulative probability in CDF");

    if (_entriesToProbs.size() == 1) {
      return Iterables.getOnlyElement(_entriesToProbs.keySet());
    }

    if (emd == null) {
      probCache = MathUtils.normalizeArray(_entriesToProbs.values(), 1.0);
      if (_sort) {
        final List<T> mapKeys = Lists.newArrayList(_entriesToProbs.keySet());
        /*
         * Sort the key index by key value, then
         * reorder the prob value array with sorted
         * index.
         */
        Collections.sort(_objIdx, new Comparator<Integer>() {
          @Override
          public int compare(Integer arg0, Integer arg1) {
            return mapKeys.get(arg0).compareTo(mapKeys.get(arg1));
          }
        });
        double[] newProbs = new double[probCache.length];
        for (int i = 0; i < probCache.length; ++i) {
          newProbs[i] = probCache[_objIdx.get(i)];
        }
        probCache = newProbs;
      }
      /*
       * Need a double array for DiscreteDistribution.  Lame. 
       */
      double[] doubleObjIdx = new double[_objIdx.size()];
      for (int i = 0; i < _objIdx.size(); ++i) {
        doubleObjIdx[i] = _objIdx.get(i);
      }
      emd = new DiscreteDistribution(doubleObjIdx, probCache, _objIdx.size());
      _entries = getSupport();
    }

    final double u = threadLocalRng.get().nextDouble();
    final int newIdx = (int) emd.inverseF(u);

    return _entries.get(newIdx);
  }

  public Multiset<T> sample(int samples) {

    Preconditions.checkArgument(samples > 0);

    final Multiset<T> sampled = HashMultiset.create(samples);

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
