package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class VehicleLocationSimulationDetails {

  private int id;

  private NycTestLocationRecord lastObservation;

  /**
   * True if the list of particles is a history of a specific particles, as
   * opposed to the set of particles active right now
   */
  private boolean history;

  private List<Particle> particles;

  private List<Particle> sampledParticles;

  private List<JourneyPhaseSummary> summaries;

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

  public boolean isHistory() {
    return history;
  }

  public void setHistory(boolean history) {
    this.history = history;
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

  public List<JourneyPhaseSummary> getSummaries() {
    return summaries;
  }

  public void setSummaries(List<JourneyPhaseSummary> summaries) {
    this.summaries = summaries;
  }
}
