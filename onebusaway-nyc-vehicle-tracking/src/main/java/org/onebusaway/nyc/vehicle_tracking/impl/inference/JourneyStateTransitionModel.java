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

  private BlockStateTransitionModel _blockStateTransitionModel;

  @Autowired
  public void setVehicleStateSensorModel(
      VehicleStateSensorModel vehicleStateSensorModel) {
    _vehicleStateSensorModel = vehicleStateSensorModel;
  }

  @Autowired
  public void setBlockStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
    _blockStateTransitionModel = blockStateTransitionModel;
  }

  /****
   * 
   * 
   ****/

  public VehicleState move(VehicleState parentState, EdgeState edgeState,
      MotionState motionState, Observation obs) {

    List<JourneyState> journeyStates = getTransitionJourneyStates(parentState,
        edgeState);

    return sampleJourneyState(parentState, edgeState, motionState,
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
      case DEADHEAD_AFTER:
        return moveDeadheadAfter(edgeState);
      case LAYOVER_AFTER:
        return moveLayoverAfter(edgeState);
      case UNKNOWN:
        return moveUnknown(edgeState);
      default:
        throw new IllegalStateException("unknown journey state: "
            + parentJourneyState.getPhase());
    }
  }

  private VehicleState sampleJourneyState(VehicleState parentState,
      EdgeState edgeState, MotionState motionState,
      List<JourneyState> journeyStates, Observation obs) {

    CDFMap<VehicleState> cdf = new CDFMap<VehicleState>();

    for (JourneyState journeyState : journeyStates) {

      BlockState blockState = _blockStateTransitionModel.transitionBlockState(
          parentState, edgeState, motionState, journeyState, obs);

      VehicleState vehicleState = new VehicleState(edgeState, motionState,
          blockState, journeyState);

      double p = _vehicleStateSensorModel.likelihood(parentState, vehicleState,
          obs);

      cdf.put(p, vehicleState);
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
        JourneyState.layoverDuring(), JourneyState.deadheadAfter(),
        JourneyState.layoverAfter());
  }

  private List<JourneyState> moveDeadheadDuring(JourneyState parentJourneyState) {

    JourneyStartState start = parentJourneyState.getData();

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(start.getJourneyStart()),
        JourneyState.layoverDuring());
  }

  private List<JourneyState> moveLayoverDuring(EdgeState edgeState) {

    return Arrays.asList(JourneyState.inProgress(),
        JourneyState.deadheadDuring(edgeState.getLocationOnEdge()),
        JourneyState.layoverDuring());
  }

  private List<JourneyState> moveDeadheadAfter(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.inProgress(), JourneyState.deadheadAfter(),
        JourneyState.layoverAfter());
  }

  private List<JourneyState> moveLayoverAfter(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.inProgress(), JourneyState.layoverAfter());
  }

  private List<JourneyState> moveUnknown(EdgeState edgeState) {

    return Arrays.asList(JourneyState.atBase(),
        JourneyState.deadheadBefore(edgeState.getLocationOnEdge()),
        JourneyState.layoverBefore(), JourneyState.inProgress());
  }
}
