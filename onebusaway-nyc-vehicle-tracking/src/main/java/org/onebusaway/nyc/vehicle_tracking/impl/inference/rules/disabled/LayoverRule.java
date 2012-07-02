/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.disabled;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.biconditional;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

// @Component
public class LayoverRule implements SensorModelRule {

  // /**
  // * How long in minutes do we let a vehicle remain in layover past its
  // * scheduled pull-out time?
  // */
  // private final DeviationModel _vehicleIsOnScheduleModel = new
  // DeviationModel(
  // 40);
  //
  // private VehicleStateLibrary _vehicleStateLibrary;
  //
  // @Autowired
  // public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary)
  // {
  // _vehicleStateLibrary = vehicleStateLibrary;
  // }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    final SensorModelResult result = new SensorModelResult("pLayover");
    final double pAtLayoverSpot = state.getBlockState() != null
        ? p(state.getBlockStateObservation().isAtPotentialLayoverSpot())
        : p(obs.isAtTerminal());

    if (EVehiclePhase.AT_BASE == phase)
      return result.addResultAsAnd("at-base", 1.0);
    /**
     * Rule: LAYOVER <=> Vehicle has not moved AND at layover location
     */

    final double pNotMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
        state.getMotionState(), obs);

    final double pLayoverState = p(EVehiclePhase.isLayover(phase));

    final double p1 = biconditional(pNotMoved * pAtLayoverSpot, pLayoverState);

    final SensorModelResult p1Result = result.addResultAsAnd(
        "LAYOVER <=> Vehicle has not moved AND at layover location", p1);

    p1Result.addResult("pNotMoved", pNotMoved);
    p1Result.addResult("pLayoverState", pLayoverState);
    p1Result.addResult("isAtPotentialLayover", pAtLayoverSpot);

    /**
     * Rule: LAYOVER_BEFORE OR LAYOVER_DURING => vehicle_is_on_schedule
     */

    // double p3 = 1.0;
    // final boolean isActiveLayoverState =
    // EVehiclePhase.isActiveLayover(phase);
    //
    // BlockState blockState = state.getBlockState();
    // if (isActiveLayoverState && blockState != null) {
    //
    // final BlockStopTimeEntry nextStop = phase == EVehiclePhase.LAYOVER_BEFORE
    // ? blockState.getBlockLocation().getNextStop()
    // :
    // VehicleStateLibrary.getPotentialLayoverSpot(blockState.getBlockLocation());
    //
    // p3 = computeVehicleIsOnScheduleProbability(obs.getTime(), blockState,
    // nextStop);
    // }
    //
    // result.addResultAsAnd(
    // "LAYOVER_BEFORE OR LAYOVER_DURING => vehicle_is_on_schedule", p3);

    return result;
  }

  // final private double schedStdDev = 80.0;
  // final private double schedMean = 15.0;
  // private double computeVehicleIsOnScheduleProbability(long timestamp,
  // BlockState blockState, BlockStopTimeEntry layoverStop) {
  //
  // final BlockInstance blockInstance = blockState.getBlockInstance();
  //
  // // No next stop? Could just be sitting...
  // if (layoverStop == null) {
  // return 0.5;
  // }
  //
  // final StopTimeEntry stopTime = layoverStop.getStopTime();
  // final long arrivalTime = blockInstance.getServiceDate()
  // + stopTime.getArrivalTime() * 1000;
  //
  // // If we still have time to spare, then no problem!
  // if (timestamp < arrivalTime)
  // return 1.0;
  //
  // final int minutesLate = (int) ((timestamp - arrivalTime) / (60 * 1000));
  // final double pSched = 1.0 - FoldedNormalDist.cdf(schedMean, schedStdDev,
  // minutesLate / 60.0);
  // // return _vehicleIsOnScheduleModel.probability(minutesLate);
  // return pSched;
  // }
}
