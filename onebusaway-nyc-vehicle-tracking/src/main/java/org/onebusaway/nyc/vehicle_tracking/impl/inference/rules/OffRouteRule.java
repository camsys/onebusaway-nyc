package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * If we get far enough off route, we should go out of service. The problem is
 * that if we are out-of-service, we've tossed our block info. Thus
 * 
 * @author bdferris
 * 
 */
@Component
public class OffRouteRule implements SensorModelRule {

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    if (parentState == null)
      return new SensorModelResult("pOffRoute (n/a)");

    BlockState parentBlockState = parentState.getBlockState();

    if (parentBlockState == null)
      return new SensorModelResult("pOffRoute (n/a)");

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    boolean offRoute = _vehicleStateLibrary.isOffBlock(
        obs.getPreviousObservation(), parentBlockState);
    boolean outOfService = EVehiclePhase.isActiveBeforeBlock(phase);

    double pOffRoute = implies(p(offRoute), p(outOfService));

    return new SensorModelResult("pOffRoute", pOffRoute);
  }
}
