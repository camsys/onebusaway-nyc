package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class VehicleLocationSimulationDetails {

  private int id;

  private NycTestLocationRecord lastObservation;

  private List<Particle> particles;
  
  private List<Particle> sampledParticles;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public NycTestLocationRecord getLastObservation() {
    return lastObservation;
  }

  public void setLastObservation(NycTestLocationRecord lastObservation) {
    this.lastObservation = lastObservation;
  }

  public List<Particle> getParticles() {
    return particles;
  }

  public void setParticles(List<Particle> particles) {
    this.particles = particles;
  }

  public List<Particle> getSampledParticles() {
    return sampledParticles;
  }

  public void setSampledParticles(List<Particle> sampledParticles) {
    this.sampledParticles = sampledParticles;
  }
}
