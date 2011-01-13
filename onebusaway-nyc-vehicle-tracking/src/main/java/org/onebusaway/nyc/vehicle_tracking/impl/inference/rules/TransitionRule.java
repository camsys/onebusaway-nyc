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

@Component
public class TransitionRule implements SensorModelRule {

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

    Observation prevObs = obs.getPreviousObservation();

    if (parentState == null || prevObs == null)
      return new SensorModelResult("pTransition (n/a)");

    JourneyState parentJourneyState = parentState.getJourneyState();
    JourneyState journeyState = state.getJourneyState();

    EVehiclePhase parentPhase = parentJourneyState.getPhase();
    EVehiclePhase phase = journeyState.getPhase();

    SensorModelResult result = new SensorModelResult("pTransition");

    /**
     * Transition Before to During => left terminal || transitioned to in
     * service
     */
    boolean transitionBeforeToDuring = EVehiclePhase.isActiveBeforeBlock(parentPhase)
        && EVehiclePhase.isActiveDuringBlock(phase);
    boolean justLeftTerminal = prevObs.isAtTerminal() && !obs.isAtTerminal();
    boolean pOutToInService = prevObs.isOutOfService() && !obs.isOutOfService();

    double pTransitionFromBeforeToDuring = implies(p(transitionBeforeToDuring),
        p(justLeftTerminal || pOutToInService));

    result.addResultAsAnd("Transition Before to During => left terminal",
        pTransitionFromBeforeToDuring);

    /**
     * Transition During to Before => out of service or at base
     */
    boolean transitionDuringToBefore = EVehiclePhase.isActiveDuringBlock(parentPhase)
        && EVehiclePhase.isActiveBeforeBlock(phase);

    boolean wasOffRoute = false;
    if (parentState.getBlockState() != null)
      wasOffRoute = _vehicleStateLibrary.isOffBlock(prevObs,
          parentState.getBlockState());

    boolean endOfBlock = false;
    BlockState blockState = state.getBlockState();
    if (blockState != null
        && blockState.getBlockLocation().getNextStop() == null)
      endOfBlock = true;

    double pTransitionFromDuringToBefore = implies(p(transitionDuringToBefore),
        p(obs.isAtBase() || obs.isOutOfService() || wasOffRoute || endOfBlock));

    result.addResultAsAnd(
        "Transition During to Before => out of service OR at base OR was off route OR just finished block",
        pTransitionFromDuringToBefore);

    /**
     * just left terminal AND in-service => active during
     */
    double pInService = implies(p(justLeftTerminal && !obs.isOutOfService()),
        p(EVehiclePhase.isActiveDuringBlock(phase)));
    result.addResultAsAnd("just left terminal AND in-service => active during",
        pInService);

    return result;
  }
}
