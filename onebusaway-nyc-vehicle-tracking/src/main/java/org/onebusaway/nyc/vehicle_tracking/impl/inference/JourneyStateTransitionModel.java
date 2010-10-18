package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Arrays;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JourneyStateTransitionModel {

  private VehicleStateSensorModel _vehicleStateSensorModel;

  @Autowired
  public void setVehicleStateSensorModel(
      VehicleStateSensorModel vehicleStateSensorModel) {
    _vehicleStateSensorModel = vehicleStateSensorModel;
  }

  /****
   * 
   * 
   ****/

  public JourneyState move(VehicleState parentState, EdgeState edgeState,
      MotionState motionState, BlockState blockState, Observation obs) {

    List<JourneyState> journeyStates = getTransitionJourneyStates(parentState,
        edgeState);

    return sampleJourneyState(parentState, edgeState, motionState, blockState,
        journeyStates, obs);
  }

  public List<JourneyState> getTransitionJourneyStates(
      VehicleState parentState, EdgeState edgeState) {

    JourneyState parentJourneyState = parentState.getJourneyState();

    switch (parentJourneyState.getPhase()) {
      case AT_BASE:
        return moveAtBase(edgeState);
      case DEADHEAD_BEFORE:
        return moveDeadheadBefore(parentJourneyState);
      case LAYOVER_BEFORE:
        return moveLayoverBefore(edgeState);
      case IN_PROGRESS:
        return moveInProgress(edgeState);
      case DEADHEAD_DURING:
        return moveDeadheadDuring(parentJourneyState);
      case LAYOVER_DURING:
        return moveLayoverDuring(edgeState);
      case OFF_ROUTE:
        return moveOffRoute(edgeState);
      case DEADHEAD_AFTER:
        return moveDeadheadAfter(edgeState);
      case UNKNOWN:
        return moveUnknown(edgeState);
      default:
        throw new IllegalStateException("unknown journey state: "
            + parentJourneyState.getPhase());
    }
  }

  private JourneyState sampleJourneyState(VehicleState parentState,
      EdgeState edgeState, MotionState motionState, BlockState blockState,
      List<JourneyState> journeyStates, Observation obs) {

    CDFMap<JourneyState> cdf = new CDFMap<JourneyState>();

    for (JourneyState journeyState : journeyStates) {

      VehicleState vehicleState = new VehicleState(edgeState, motionState,
          blockState, journeyState);

      double p = _vehicleStateSensorModel.likelihood(parentState, vehicleState,
          obs);

      cdf.put(p, journeyState);
    }

    return cdf.sample();
  }

  private List<JourneyState> moveAtBase(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(), JourneyState.layoverBefore(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.inProgress());
  }

  private List<JourneyState> moveDeadheadBefore(JourneyState parentJourneyState) {

    JourneyStartState start = parentJourneyState.getData();

    return Arrays.asList(JourneyState.atBase(), JourneyState.layoverBefore(),
        JourneyState.deadheadBefore(start.getJourneyStart()),
        JourneyState.inProgress());
  }

  private List<JourneyState> moveLayoverBefore(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(), JourneyState.layoverBefore(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.inProgress());
  }

  private List<JourneyState> moveInProgress(EdgeState edgeState) {

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(edgeState.getLocationOnEdge()),
        JourneyState.layoverDuring(), JourneyState.offRoute(),
        JourneyState.deadheadAfter());
  }

  private List<JourneyState> moveDeadheadDuring(JourneyState parentJourneyState) {

    JourneyStartState start = parentJourneyState.getData();

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(start.getJourneyStart()),
        JourneyState.layoverDuring(), JourneyState.offRoute());
  }

  private List<JourneyState> moveLayoverDuring(EdgeState edgeState) {

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(edgeState.getLocationOnEdge()),
        JourneyState.layoverDuring(), JourneyState.offRoute());
  }

  private List<JourneyState> moveOffRoute(EdgeState edgeState) {

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(edgeState.getLocationOnEdge()),
        JourneyState.layoverDuring(), JourneyState.offRoute(),
        JourneyState.deadheadAfter());
  }

  private List<JourneyState> moveDeadheadAfter(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.layoverBefore(), JourneyState.inProgress(),
        JourneyState.deadheadAfter());
  }

  private List<JourneyState> moveUnknown(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.layoverBefore(), JourneyState.inProgress());
  }
}
