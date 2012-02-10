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

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
public class InProgressRule implements SensorModelRule {

  private DeviationModel _nearbyTripSigma = new DeviationModel(50.0);

  BlockStateService _blockStateService;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();

    /**
     * Basically, we say that in-progress states correspond more to nearby trips
     * and good block locations. As well, we're informed of the likelihood of
     * not-in-progress states, by way of having more of the opposite properties.
     */
    
    /*
     * When we have no block-state, we use the best state with a 
     * route that matches the observations DSC-implied route, if any.
     */
    if (blockState == null
        || phase == EVehiclePhase.DEADHEAD_BEFORE) {
//      BestBlockStates bestStates = _blockStateService.getBestBlockStateForRoute(obs);
//      if (bestStates != null)
//        blockState = bestStates.getBestLocation();
    }
    
    if (!(phase == EVehiclePhase.IN_PROGRESS
        || phase == EVehiclePhase.DEADHEAD_BEFORE 
        || phase == EVehiclePhase.DEADHEAD_DURING)
        || blockState == null)
      return new SensorModelResult("pInProgress (n/a)");

    SensorModelResult result = new SensorModelResult("pInProgress");

    /**
     * Rule: IN_PROGRESS => block location is close to the best possible?
     */
    double pBlockLocation = library.computeBlockLocationProbability(
        parentState, blockState, obs);

    /**
     * Rule: IN_PROGRESS => trip in close range
     */
    CoordinatePoint p1 = blockState.getBlockLocation().getLocation();

    ProjectedPoint p2 = obs.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());

    double pTripInRange = _nearbyTripSigma.probability(d);

    if (phase == EVehiclePhase.DEADHEAD_BEFORE
        || phase == EVehiclePhase.DEADHEAD_DURING) {
      // double invResult = 1.0 - pBlockLocation * pTripInRange;
      double invResult = 1.0 - pTripInRange;
      result.addResultAsAnd("pNotInProgress", invResult);
    } else {
      result.addResultAsAnd("pBlockLocation", pBlockLocation);
      result.addResultAsAnd("pTripInRange", pTripInRange);
    }

    return result;
  }

}
