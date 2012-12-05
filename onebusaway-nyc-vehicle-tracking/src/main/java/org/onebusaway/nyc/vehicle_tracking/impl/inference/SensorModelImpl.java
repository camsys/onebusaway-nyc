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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

@Component
public class SensorModelImpl implements SensorModel<Observation> {

  private List<SensorModelRule> _rules = Collections.emptyList();

  private SensorModelSupportLibrary _sensorModelLibrary;

  @PostConstruct
  @Refreshable(dependsOn = {RefreshableResources.NARRATIVE_DATA})
  public void setup() {
  }

  @Autowired
  public void setSensorModelLibrary(SensorModelSupportLibrary sensorModelLibrary) {
    _sensorModelLibrary = sensorModelLibrary;
  }

  /****
   * {@link SensorModel} Interface
   ****/
  @Override
  public SensorModelResult likelihood(Particle particle,
      Observation observation,
      Map<Entry<VehicleState, VehicleState>, SensorModelResult> cache)
          throws BadProbabilityParticleFilterException {

    final VehicleState state = particle.getData();
    VehicleState parentState = null;
    final Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    final Entry<VehicleState, VehicleState> key = new AbstractMap.SimpleImmutableEntry<VehicleState, VehicleState>(
        state, parentState);

    SensorModelResult result = cache.get(key);
    if (result == null) {
      result = likelihood(parentState, state, observation);
      cache.put(key, result);
    }

    return result;
  }

  @Override
  public SensorModelResult likelihood(Particle particle, Observation observation) throws BadProbabilityParticleFilterException {

    final VehicleState state = particle.getData();
    VehicleState parentState = null;
    final Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    final SensorModelResult result = likelihood(parentState, state, observation);

    return result;
  }

  @Autowired
  public void setRules(List<SensorModelRule> rules) {
    _rules = rules;
  }

  /****
   * {@link SensorModelImpl} Interface
   * @throws BadProbabilityParticleFilterException TODO
   ****/

  public SensorModelResult likelihood(VehicleState parentState,
      VehicleState state, Observation obs) throws BadProbabilityParticleFilterException {

    final SensorModelResult result = new SensorModelResult("pTotal", 1.0);
    final Context context = new Context(parentState, state, obs);

    for (final SensorModelRule rule : _rules) {
      final SensorModelResult r = rule.likelihood(_sensorModelLibrary, context);
      result.addResultAsAnd(r);
    }

    return result;
  }
}
