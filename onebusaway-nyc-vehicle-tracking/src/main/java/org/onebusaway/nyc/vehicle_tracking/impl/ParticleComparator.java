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