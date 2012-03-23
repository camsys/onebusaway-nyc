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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import java.util.Collection;

/**
 * Particle motion model interface for defining the strategy for moving
 * particles.
 * 
 * @author bdferris
 * 
 * @param <OBS>
 */
public interface MotionModel<OBS> {

  /**
   * @param parent the parent of the new particle
   * @param timestamp timestamp of the new particle
   * @param timeElapsed time elapsed since last move
   * @param obs observation at the given timestamp
   * @param results TODO
   * @throws Exception TODO
   */
  public void move(Particle parent, double timestamp, double timeElapsed,
      OBS obs, Collection<Particle> results);

  /**
   * This version of move will utilize the passed cache to avoid recalculating
   * results. Especially useful for multiple null block-states.
   * 
   * @param parent
   * @param timestamp
   * @param timeElapsed
   * @param obs
   * @param results
   * @param cache
   * @return 
   */
  Multiset<Particle> move(Multiset<Particle> particles, double timestamp, double timeElapsed,
      OBS obs);
}
