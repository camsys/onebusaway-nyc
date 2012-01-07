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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math.util.MathUtils;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

public class CategoricalDist<T> {

  private double _cumulativeProb = 0.0;

  final static private Random rng = new Random();

  private List<Double> _objIdx = new ArrayList<Double>();
//  TreeMap<T, Double> _entriesToProbs;
  Map<T, Double> _entriesToProbs = new HashMap<T, Double>();
  private List<T> _entries;

  DiscreteDistribution emd;
  
//  public CategoricalDist() {
//    _entriesToProbs = new TreeMap<T, Double>(Ordering.usingToString());
//  }
//  
//  public CategoricalDist(Ordering<T> order) {
//    _entriesToProbs = new TreeMap<T, Double>(order);
//  }
    
  static public void setSeed(long seed) {
    rng.setSeed(seed);
  }

  synchronized public List<T> getSupport() {
    return new ArrayList<T>(_entriesToProbs.keySet());
  }

  synchronized public void put(double prob, T object) {

    _cumulativeProb += prob;
    Double currentProb = _entriesToProbs.get(object);
    
    if (currentProb == null) {
      _entriesToProbs.put(object, prob);
      _objIdx.add((double)_objIdx.size());
      _entries = getSupport(); 
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

  synchronized public T sample() {

    if (_entriesToProbs.isEmpty())
      throw new IllegalStateException("No entries in the CDF");

    if (_cumulativeProb == 0.0)
      throw new IllegalStateException("No cumulative probability in CDF");

    if (_entriesToProbs.size() == 1) {
      return _entriesToProbs.keySet().iterator().next();
//      return _entriesToProbs.firstKey();
    }
    
    if (emd == null) {
      double[] probs = MathUtils.normalizeArray(Doubles.toArray(_entriesToProbs.values()), 1.0);
      emd = new DiscreteDistribution(Doubles.toArray(_objIdx), probs, _objIdx.size());
    }
    
    double u = rng.nextDouble();
    int newIdx = (int) emd.inverseF(u);
    
    return _entries.get(newIdx);
  }

  synchronized public List<T> sample(int samples) {

    if (_entriesToProbs.isEmpty())
      throw new IllegalStateException("No entries in the CDF map");

    if (_cumulativeProb == 0.0)
      throw new IllegalStateException("No cumulative probability in CDF");

    if (samples == 0)
      return Collections.emptyList();

    List<T> sampled = new ArrayList<T>(samples);

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

  synchronized public double density(T thisState) {
    return _entriesToProbs.get(thisState);
  }
}
