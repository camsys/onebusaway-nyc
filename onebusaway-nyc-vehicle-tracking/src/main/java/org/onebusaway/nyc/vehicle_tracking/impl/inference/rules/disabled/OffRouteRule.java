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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.disabled;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * If we get far enough off route, we should go out of service. The problem is
 * that if we are out-of-service, we've tossed our block info. Thus
 * 
 * @author bdferris
 * 
 */
// @Component
public class OffRouteRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState parentState = context.getParentState();
    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    if (parentState == null)
      return new SensorModelResult("pOffRoute (n/a)");

    final BlockState parentBlockState = parentState.getBlockState();

    if (parentBlockState == null)
      return new SensorModelResult("pOffRoute (n/a)");

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    final boolean offRoute = _vehicleStateLibrary.isOffBlock(
        obs.getPreviousObservation(), parentBlockState);
    final boolean outOfService = EVehiclePhase.isActiveBeforeBlock(phase);

    final double pOffRoute = implies(p(offRoute), p(outOfService));

    return new SensorModelResult("pOffRoute", pOffRoute);
  }
}
