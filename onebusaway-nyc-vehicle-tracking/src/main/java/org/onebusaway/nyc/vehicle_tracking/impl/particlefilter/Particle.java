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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
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
  
  private static class ParticleDataOrdering extends Ordering<Object> {

    @Override
    public int compare(Object arg0, Object arg1) {
      if ((arg0 instanceof VehicleState)
          && (arg1 instanceof VehicleState)) {
        VehicleState vstate0 = (VehicleState) arg0;
        VehicleState vstate1 = (VehicleState) arg1;
        return vstate0.compareTo(vstate1);
      }
      return 0;
    }
    
  }
  
  private static ParticleDataOrdering _particleDataOrdering = new ParticleDataOrdering();
  
  private static int oneParticleCompareTo(Particle t, Particle o) {
    if (t == o)
      return 0;
    
    if (t == null) {
      if (o != null)
        return -1;
    } else if (o == null) {
      return 1;
    }
    
    int particleComp = ComparisonChain.start()
        .compare(t.getData(), o.getData(), _particleDataOrdering.nullsLast())
        .compare(t.getWeight(), o.getWeight())
        .compare(t.getTimestamp(), o.getTimestamp())
        .result();
    return particleComp;
  }

  @Override
  public int compareTo(Particle o) {
    
    int res = oneParticleCompareTo(this, o);
    
    if (res != 0)
      return res;
    
    if (o != null)
      res = oneParticleCompareTo(this._parent, o._parent);
      
    return res;
  }

  private static int oneParticleHashCode(Particle p) {
    final int prime = 31;
    int result = 0;
    
    int pdataHashCode = 0;
    long temp1 = 0;
    long temp2 = 0;
    if (p != null) {
      if (p._data != null) {
        if ((p._data instanceof VehicleState)) {
          pdataHashCode = ((VehicleState) p._data).hashCode();
        } else {
          pdataHashCode = p._data.hashCode();
        }
      }
      temp1 = Double.doubleToLongBits(p._timestamp);
      temp2 = Double.doubleToLongBits(p._weight);
    }
    result = pdataHashCode;
    result = prime * result + (int) (temp1 ^ (temp1 >>> 32));
    result = prime * result + (int) (temp2 ^ (temp2 >>> 32));
    return result;
    
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * oneParticleHashCode(this);
    result = prime * oneParticleHashCode(this._parent); 
    return result;
  }

  private static boolean oneParticleEquals(Particle thisObj, Object obj) {
    if (thisObj == obj) {
      return true;
    }
    
    if (thisObj == null) {
      if (obj != null)
        return false;
    } else if (obj == null)
      return false;
    
    if (!(obj instanceof Particle)) {
      return false;
    }
    Particle other = (Particle) obj;
    if (thisObj._data == null) {
      if (other._data != null) {
        return false;
      }
    } else if (other._data != null) {
      if (thisObj._data instanceof VehicleState) {
        VehicleState vdata = (VehicleState) thisObj._data;
        VehicleState vother = (VehicleState) other._data;
      
        if (!vdata.equals(vother)) {
          return false;
        }
      } else if (!thisObj._data.equals(other._data)) {
        return false;
      }
      
    } else {
      return false;
    }
    
    if (Double.doubleToLongBits(thisObj._timestamp) != Double.doubleToLongBits(other._timestamp)) {
      return false;
    }
    if (Double.doubleToLongBits(thisObj._weight) != Double.doubleToLongBits(other._weight)) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean equals(Object obj) {
    
    if (!oneParticleEquals(this, obj))
      return false;
    
    Particle other = (Particle) obj;
    if (this._parent == null) {
      if (other._parent != null)
        return false;
    } else if (!oneParticleEquals(this._parent, other._parent)){
      return false;
    }
      
    return true;
  }
}
