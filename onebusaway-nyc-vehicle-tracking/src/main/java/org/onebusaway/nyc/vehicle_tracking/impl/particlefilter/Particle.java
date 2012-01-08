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
package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import com.google.common.primitives.Longs;

import java.io.Serializable;

/**
 * Particle object has information about time, weight, the parent particle, and
 * any associated data.
 * 
 * @author bdferris
 * @see ParticleFilter
 */
public class Particle implements Serializable, Comparable<Particle> {

  private static final long serialVersionUID = 1L;

  private double _timestamp;

  private double _weight = 1.0;
  
  private SensorModelResult _result;

  private Particle _parent;
  
  private int _index;

  private Object _data;

  public Particle(double timestamp) {
    this(timestamp, null);
  }

  public Particle(double timestamp, Particle parent) {
    this(timestamp, parent, 1.0);
  }

  public Particle(double timestamp, Particle parent, double weight) {
    this(timestamp, parent, weight, null);
  }
  
  public Particle(double timestamp, Particle parent, double weight, Object data) {
    _timestamp = timestamp;
    _parent = parent;
    _weight = weight;
    _data = data;
    
    /*
     * This is done so that we're not keeping
     * the entire trajectory of particles in
     * memory.  All we really need is a Markov
     * chain.
     * Debug mode will allow it for bookkeeping.
     */
    if (!ParticleFilter.getDebugEnabled()
        && parent != null)
      parent.clearParent();
  }

  private void clearParent() {
    _parent = null;
  }

  public Particle(Particle particle) {
    _timestamp = particle.getTimestamp();
    _parent = particle.getParent();
    _weight = particle.getWeight();
    _data = particle.getData();
  }

  public double getTimestamp() {
    return _timestamp;
  }

  public void setTimestamp(double timestamp) {
    _timestamp = timestamp;
  }

  public double getWeight() {
    return _weight;
  }

  public void setWeight(double weight) {
    _weight = weight;
  }
  
  public SensorModelResult getResult() {
    return _result;
  }
  
  public void setResult(SensorModelResult result) {
    _result = result;
  }

  public Particle getParent() {
    return _parent;
  }

  public void setParent(Particle parent) {
    _parent = parent;
  }
  
  public int getIndex() {
    return _index;
  }
  
  public void setIndex(int index) {
    _index = index;
  }

  @SuppressWarnings("unchecked")
  public <T> T getData() {
    return (T) _data;
  }

  public void setData(Object data) {
    _data = data;
  }

  public Particle cloneParticle() {
    return new Particle(this);
  }
  
  public Particle advanceParticle(long timestamp) {
    Particle p = new Particle(timestamp, this, _weight, _data);
    return p;
  }

  @Override
  public String toString() {
    return "Particle(time=" + _timestamp + " weight=" + _weight + " data="
        + _data + ")";
  }

  @Override
  public int compareTo(Particle o) {
    if (this == o)
      return 0;
    
    int timeComp = Double.compare(this.getTimestamp(), o.getTimestamp());
    if (timeComp != 0)
      return timeComp;
    
    int weightComp = Double.compare(_weight, o._weight);
    if (weightComp != 0)
      return weightComp;
    
    Particle oParent = o.getParent();
    if (this._parent != null && oParent != null) {
      /*
       * so we don't cause any crazy recursion...
       */
      int pTimeComp = Double.compare(this._parent.getTimestamp(), oParent.getTimestamp());
      if (pTimeComp != 0)
        return pTimeComp;
      
      int pWeightComp = Double.compare(this._parent.getWeight(), oParent.getWeight());
      if (pWeightComp != 0)
        return pWeightComp;
      
    } else if (this._parent != null && oParent == null) {
      return 1;
    } else if (this._parent == null && oParent != null) {
      return -1;
    }
    
    return 0;
  }
}
