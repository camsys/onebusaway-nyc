package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

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
    _timestamp = timestamp;
    _parent = parent;
    _weight = weight;
  }
  
  public Particle(double timestamp, Particle parent, double weight, Object data) {
    _timestamp = timestamp;
    _parent = parent;
    _weight = weight;
    _data = data;
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
    Particle p = new Particle(_timestamp);
    p.setData(_data);
    p.setParent(this);
    p.setWeight(_weight);
    return p;
  }

  @Override
  public String toString() {
    return "Particle(time=" + _timestamp + " weight=" + _weight + " data="
        + _data + ")";
  }

  @Override
  public int compareTo(Particle o) {
    return Double.compare(_weight, o._weight);
  }
}
