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

import com.google.common.collect.Multiset;

/**
 * Factory interface for creating an initial set of particles from an initial
 * observation
 * 
 * @param <OBS> the observation type
 * @author bdferris
 * 
 * @see ParticleFilter
 */
public interface ParticleFactory<OBS> {

  /**
   * @param timestamp time of the initial observation
   * @param observation the initial observation
   * @return the initial list of particles
   * @throws ParticleFilterException 
   */
  public Multiset<Particle> createParticles(double timestamp, OBS observation) throws ParticleFilterException;
}
