package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.*;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AtBaseRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    boolean isAtBase = _vehicleStateLibrary.isAtBase(obs.getLocation());

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    /**
     * RULE: AT_BASE <=> bus located at the base
     */
    double pAtBase = biconditional(p(phase == EVehiclePhase.AT_BASE),
        p(isAtBase));

    return new SensorModelResult("pAtBase", pAtBase);
  }
}
