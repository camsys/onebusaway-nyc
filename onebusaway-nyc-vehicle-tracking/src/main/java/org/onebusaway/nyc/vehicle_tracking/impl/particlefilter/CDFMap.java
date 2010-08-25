package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.SortedMap;
import java.util.TreeMap;

public class CDFMap<T> {

  private double _cumulativeProb = 0.0;

  private SortedMap<Double, T> _entries = new TreeMap<Double, T>();

  public void put(double prob, T object) {
    _entries.put(new Double(_cumulativeProb), object);
    _cumulativeProb += prob;
  }

  public T sample() {

    if (_entries.isEmpty())
      throw new IllegalStateException("No entries in the CDF map");

    double index = Math.random() * _cumulativeProb;

    if (index == 0.0)
      return _entries.get(_entries.firstKey());

    SortedMap<Double, T> map = _entries.headMap(new Double(index));
    return _entries.get(map.lastKey());
  }

  public boolean isEmpty() {
    return _entries.isEmpty();
  }

  public int size() {
    return _entries.size();
  }
}
