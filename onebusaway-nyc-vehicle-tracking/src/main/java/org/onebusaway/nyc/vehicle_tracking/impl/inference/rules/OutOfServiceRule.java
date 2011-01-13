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
public class OutOfServiceRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    SensorModelResult result = new SensorModelResult("pService");

    /**
     * out of service => AT_BASE or DEADHEAD_BEFORE or LAYOVER_BEFORE
     */

    boolean outOfService = obs.isOutOfService();
    boolean beforeState = EVehiclePhase.isActiveBeforeBlock(phase);

    double pOutOfService = implies(p(outOfService), p(beforeState));

    result.addResultAsAnd("pOutOfService", pOutOfService);

    return result;
  }
}
