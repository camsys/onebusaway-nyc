package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.stereotype.Component;

@Component
public class DestinationSignCodeRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    String observedDsc = obs.getLastValidDestinationSignCode();

    /**
     * If we haven't yet seen a valid DSC
     */
    if (observedDsc == null)
      return new SensorModelResult("pDestinationSignCode: no DSC set", 1.0);

    SensorModelResult result = new SensorModelResult("pDestinationSignCode");

    /**
     * Rule: out-of-service DSC => ! IN_PROGRESS
     */

    double p1 = implies(p(obs.isOutOfService()),
        p(phase != EVehiclePhase.IN_PROGRESS));

    result.addResultAsAnd("out-of-service DSC => ! IN_PROGRESS", p1);

    return result;
  }

}
