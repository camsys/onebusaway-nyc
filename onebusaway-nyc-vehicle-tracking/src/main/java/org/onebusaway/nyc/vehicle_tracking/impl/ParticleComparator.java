package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.Comparator;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;

public class ParticleComparator implements Comparator<Particle> {
  
  public static final ParticleComparator INSTANCE = new ParticleComparator();

  @Override
  public int compare(Particle o1, Particle o2) {
    return Double.compare(o2.getWeight(), o1.getWeight());
  }
}