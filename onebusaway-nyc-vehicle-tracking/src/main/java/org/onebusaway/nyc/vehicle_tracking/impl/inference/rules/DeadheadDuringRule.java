package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.not;
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
public class DeadheadDuringRule implements SensorModelRule {

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

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    if (phase != EVehiclePhase.DEADHEAD_DURING || blockState == null)
      return new SensorModelResult("pDeadheadDuring (n/a)");

    SensorModelResult result = new SensorModelResult("pDeadheadDuring");

    /**
     * Rule: DEADHEAD_DURING <=> Vehicle has moved AND at layover location
     */

    double pMoved = not(library.computeVehicelHasNotMovedProbability(
        state.getMotionState(), obs));

    double pAtLayoverLocation = p(_vehicleStateLibrary.isAtPotentialLayoverSpot(
        state, obs));

    double p1 = pMoved * pAtLayoverLocation;
    result.addResultAsAnd(
        "DEADHEAD_DURING <=> Vehicle has moved AND at layover location", p1);

    /**
     * Rule: DEADHEAD_DURING => not right on block
     */
    double pDistanceFromBlock = library.computeDeadheadDistanceFromBlockProbability(
        obs, blockState);
    result.addResultAsAnd("DEADHEAD_DURING => not right on block",
        pDistanceFromBlock);

    /**
     * Rule: DEADHEAD_DURING => resume block on time
     */
    double pStartBlockOnTime = library.computeStartOrResumeBlockOnTimeProbability(
        state, obs);
    result.addResultAsAnd("DEADHEAD_DURING => resume block on time",
        pStartBlockOnTime);

    /**
     * Rule: DEADHEAD_DURING => served some part of block
     */
    double pServedSomePartOfBlock = library.computeProbabilityOfServingSomePartOfBlock(state.getBlockState());
    result.addResultAsAnd("DEADHEAD_DURING => served some part of block",
        pServedSomePartOfBlock);

    return result;
  }

}
