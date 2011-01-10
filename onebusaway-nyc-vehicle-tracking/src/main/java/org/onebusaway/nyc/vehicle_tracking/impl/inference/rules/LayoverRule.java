package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.*;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LayoverRule implements SensorModelRule {

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

    SensorModelResult result = new SensorModelResult("pLayover");

    /**
     * Rule: LAYOVER <=> Vehicle has not moved AND at layover location
     */

    double pNotMoved = library.computeVehicelHasNotMovedProbability(
        state.getMotionState(), obs);

    double pAtLayoverLocation = p(_vehicleStateLibrary.isAtPotentialLayoverSpot(
        state, obs));

    double pLayoverState = p(EVehiclePhase.isLayover(phase));

    double p1 = biconditional(pNotMoved * pAtLayoverLocation, pLayoverState);

    SensorModelResult p1Result = result.addResultAsAnd(
        "LAYOVER_AFTER => ! pAtLayoverLocation", p1);

    // For diagnostics
    p1Result.addResult("pNotMoved", pNotMoved);
    p1Result.addResult("pAtLayoverLocation", pAtLayoverLocation);
    p1Result.addResult("pLayoverState", pLayoverState);

    /**
     * Rule: LAYOVER_DURING => made some progress on the block
     */

    double pLayoverDuring = p(phase == EVehiclePhase.LAYOVER_DURING);
    double pServedSomePartOfBlock = library.computeProbabilityOfServingSomePartOfBlock(blockState);

    double p2 = implies(pLayoverDuring, pServedSomePartOfBlock);
    
    SensorModelResult p2Result = result.addResultAsAnd("LAYOVER_DURING => made some progress on the block", p2);
    p2Result.addResult("pLayoverDuring", pLayoverDuring);
    p2Result.addResult("pServedSomePartOfBlock",pServedSomePartOfBlock);

    /**
     * Rule: LAYOVER_BEFORE OR LAYOVER_DURING => vehicle_is_on_schedule
     */

    double p3 = 1.0;
    boolean isActiveLayoverState = EVehiclePhase.isActiveLayover(phase);

    if (isActiveLayoverState && blockState != null) {

      BlockStopTimeEntry nextStop = phase == EVehiclePhase.LAYOVER_BEFORE
          ? blockState.getBlockLocation().getNextStop()
          : _vehicleStateLibrary.getPotentialLayoverSpot(blockState.getBlockLocation());

      p3 = library.computeVehicleIsOnScheduleProbability(obs.getTime(),
          blockState, nextStop);
    }
    
    result.addResultAsAnd(
        "LAYOVER_BEFORE OR LAYOVER_DURING => vehicle_is_on_schedule", p3);
    
    return result;
  }

}
