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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.stereotype.Component;

@Component
public class PriorRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    /**
     * Technically, we might better weight these from prior training data, but
     * for now we want to slightly prefer being in service, not out of service
     */
    if (phase == EVehiclePhase.DEADHEAD_DURING
        || phase == EVehiclePhase.DEADHEAD_BEFORE)
      return new SensorModelResult("pPrior", 0.5);
    else if (EVehiclePhase.isActiveAfterBlock(phase))
      return new SensorModelResult("pPrior", 0.7);
    else
      return new SensorModelResult("pPrior", 1.0);
  }

}
