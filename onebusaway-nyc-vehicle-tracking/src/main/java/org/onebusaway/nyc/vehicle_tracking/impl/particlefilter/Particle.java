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

import java.io.Serializable;

import org.apache.commons.math.util.FastMath;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

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

  private double _logWeight = 0.0;

  transient private SensorModelResult _result;

  private Particle _parent;

  private int _index;

  private Object _data;

  transient private SensorModelResult _transResult;

  private Multiset<Particle> _transitions = null;

  private double _logNormedWeight = 0.0;

  public Particle(double timestamp) {
    this(timestamp, null);
  }

  public Particle(double timestamp, Particle parent) {
    this(timestamp, parent, 0.0);
  }

  public Particle(double timestamp, Particle parent, double logWeight) {
    this(timestamp, parent, logWeight, null);
  }

  public Particle(double timestamp, Particle parent, double logWeight,
      Object data) {
    _timestamp = timestamp;
    _parent = parent;
    _logWeight = logWeight;
    _data = data;

    /*
     * This is done so that we're not keeping the entire trajectory of particles
     * in memory. All we really need is a Markov chain. Debug mode will allow it
     * for bookkeeping.
     */
    if (parent != null) {
      if (!ParticleFilter.getDebugEnabled()) {
        parent.clearParent();
      } 
    }
  }

  private void clearParent() {
    _parent = null;
  }

  public Particle(Particle particle) {
    _timestamp = particle.getTimestamp();
    _parent = particle.getParent();
    _logWeight = particle.getLogWeight();
    _data = particle.getData();
    _transitions = particle._transitions;
  }

  public double getTimestamp() {
    return _timestamp;
  }

  public void setTimestamp(double timestamp) {
    _timestamp = timestamp;
  }

  public double getWeight() {
    return FastMath.exp(_logWeight);
  }

  public void setWeight(double weight) throws BadProbabilityParticleFilterException {
    if (Double.isNaN(weight) || weight < 0d || weight > 1d)
      throw new BadProbabilityParticleFilterException("invalid weight assignment: weight=" + weight);
    _logWeight = FastMath.log(weight);
  }

  public void setLogWeight(double logWeight) throws BadProbabilityParticleFilterException {
    if (Double.isNaN(logWeight) || logWeight > 0d)
      throw new BadProbabilityParticleFilterException("invalid weight assignment: logWeight=" + logWeight);
    _logWeight = logWeight;
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
    final Particle p = new Particle(timestamp, this, _logWeight, _data);
    return p;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("Particle").add("time", _timestamp).add(
        "weight", _logWeight).add("data", _data).add("parentState",
        _parent != null ? _parent.getData() : null).toString();
  }

  private static class ParticleDataOrdering extends Ordering<Object> {

    @Override
    public int compare(Object arg0, Object arg1) {
      if ((arg0 instanceof VehicleState) && (arg1 instanceof VehicleState)) {
        final VehicleState vstate0 = (VehicleState) arg0;
        final VehicleState vstate1 = (VehicleState) arg1;
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
    } else {
      final int particleComp = ComparisonChain.start().compare(t.getTimestamp(),
          o.getTimestamp(), Ordering.natural().reverse()).compare(
          t.getLogWeight(), o.getLogWeight(), Ordering.natural().reverse()).compare(
          t.getData(), o.getData(), _particleDataOrdering.nullsLast()).result();
      return particleComp;
    }
    
    throw new IllegalStateException("Illegal particle comparison");
  }

  @Override
  public int compareTo(Particle o) {

    int res = oneParticleCompareTo(this, o);

    if (res != 0)
      return res;

    final Particle oParent = (o != null ? o._parent : null);
    res = oneParticleCompareTo(this._parent, oParent);

    return res;
  }

  private static int oneParticleHashCode(Particle p) {
    final int prime = 31;
    int result = 1;

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
      temp2 = Double.doubleToLongBits(p._logWeight);
    }
    result = prime * result + pdataHashCode;
    result = prime * result + (int) (temp1 ^ (temp1 >>> 32));
    result = prime * result + (int) (temp2 ^ (temp2 >>> 32));
    return result;

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + oneParticleHashCode(this);
    result = prime * result + oneParticleHashCode(this._parent);
    return result;
  }

  private static boolean oneParticleEquals(Particle thisObj, Object obj) {
    if (thisObj == obj) {
      return true;
    }

    if (thisObj == null) {
      if (obj != null)
        return false;
    } else if (obj == null) {
      return false;
    } else {
      if (!(obj instanceof Particle)) {
        return false;
      }
      final Particle other = (Particle) obj;
      if (Double.doubleToLongBits(thisObj._timestamp) != Double.doubleToLongBits(other._timestamp)) {
        return false;
      }
      if (Double.doubleToLongBits(thisObj._logWeight) != Double.doubleToLongBits(other._logWeight)) {
        return false;
      }
      if (thisObj._data == null) {
        if (other._data != null) {
          return false;
        }
      } else if (other._data != null) {
        if (thisObj._data instanceof VehicleState) {
          final VehicleState vdata = (VehicleState) thisObj._data;
          final VehicleState vother = (VehicleState) other._data;
  
          if (!vdata.equals(vother)) {
            return false;
          }
        } else if (!thisObj._data.equals(other._data)) {
          return false;
        }
  
      } else {
        return false;
      }
      
      return true;
    }

    throw new IllegalStateException("Illegal particle equality check");
  }

  @Override
  public boolean equals(Object obj) {

    if (!oneParticleEquals(this, obj))
      return false;

    final Particle other = (Particle) obj;
    if (!oneParticleEquals(this._parent, other._parent))
      return false;

    return true;
  }

  public void setTransResult(SensorModelResult transResult) {
    this._transResult = transResult;
    if (_result != null) {
      this._result.addResultAsAnd(transResult);
    } else {
      this._result = transResult;
    }
  }

  public SensorModelResult getTransResult() {
    return this._transResult;
  }

  public double getLogWeight() {
    return _logWeight;
  }

  public Multiset<Particle> getTransitions() {
    return _transitions;
  }

  public void setTransitions(Multiset<Particle> transitions) {
    _transitions = transitions;
  }

  public void setLogNormedWeight(double d) throws BadProbabilityParticleFilterException {
    if (Double.isNaN(d) || d > 0d)
      throw new BadProbabilityParticleFilterException("invalid weight assignment: logWeight=" + d);
    _logNormedWeight = d;
  }

  public double getLogNormedWeight() {
    return _logNormedWeight;
  }

}
