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
    if (EVehiclePhase.isActiveDuringBlock(phase))
      return new SensorModelResult("pPrior", 1.0);
    else
      return new SensorModelResult("pPrior", 0.5);
  }

}
