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
package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

public class VehicleLocationDetails {

  private int id;

  private String vehicleId;

  private NycVehicleLocationRecord lastObservation;

  /**
   * True if the list of particles is a history of a specific particles, as
   * opposed to the set of particles active right now
   */
  private boolean history;

  private List<Particle> particles;

  private List<Particle> sampledParticles;

  private List<JourneyPhaseSummary> summaries;

  /**
   * True when the particle filter has had a failure
   */
  private boolean particleFilterFailure;

  /**
   * True when the particle filter has had a failure
   */
  private boolean particleFilterFailureActive;
  
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public NycVehicleLocationRecord getLastObservation() {
    return lastObservation;
  }

  public void setLastObservation(NycVehicleLocationRecord lastObservation) {
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

  public boolean isParticleFilterFailure() {
    return particleFilterFailure;
  }

  public void setParticleFilterFailure(boolean particleFilterFailure) {
    this.particleFilterFailure = particleFilterFailure;
  }

  public boolean isParticleFilterFailureActive() {
    return particleFilterFailureActive;
  }

  public void setParticleFilterFailureActive(boolean particleFilterFailureActive) {
    this.particleFilterFailureActive = particleFilterFailureActive;
  }
}
