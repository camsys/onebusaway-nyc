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
package org.onebusaway.nyc.vehicle_tracking.model.simulator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import java.util.Collections;
import java.util.List;

public class VehicleLocationDetails {

  private int id;

  private AgencyAndId vehicleId;

  private NycRawLocationRecord lastObservation;

  /**
   * True if the list of particles is a history of a specific particles, as
   * opposed to the set of particles active right now
   */
  private boolean history;

  private Multiset<Particle> particles;

  private Multiset<Particle> sampledParticles;

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

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public NycRawLocationRecord getLastObservation() {
    return lastObservation;
  }

  public void setLastObservation(NycRawLocationRecord lastObservation) {
    this.lastObservation = lastObservation;
  }

  public boolean isHistory() {
    return history;
  }

  public void setHistory(boolean history) {
    this.history = history;
  }

  public List<Entry<Particle>> getParticles() {
    return Lists.newArrayList(particles.entrySet());
  }

  public void setParticles(Multiset<Particle> particles) {
    this.particles = particles;
  }

  public List<Entry<Particle>> getSampledParticles() {
    return sampledParticles == null ? Collections.<Entry<Particle>>emptyList() : Lists.newArrayList(sampledParticles.entrySet());
  }

  public double getSampleSize() {
    return particles.size();
  }

  public double getEffectiveSampleSize() {
    try {
      return ParticleFilter.getEffectiveSampleSize(particles);
    } catch (final ParticleFilterException e) {
      e.printStackTrace();
    }
    return Double.NaN;
  }

  public void setSampledParticles(Multiset<Particle> sampledParticles2) {
    this.sampledParticles = sampledParticles2;
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
