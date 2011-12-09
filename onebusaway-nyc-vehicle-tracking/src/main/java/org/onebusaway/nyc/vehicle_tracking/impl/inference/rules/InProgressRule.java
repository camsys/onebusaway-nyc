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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.springframework.stereotype.Component;

@Component
public class InProgressRule implements SensorModelRule {

  private DeviationModel _nearbyTripSigma = new DeviationModel(50.0);
  
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
     * These probabilities only apply if are IN_PROGRESS and have a block state
     */
    if (phase != EVehiclePhase.IN_PROGRESS || blockState == null)
      return new SensorModelResult("pInProgress (n/a)");

    SensorModelResult result = new SensorModelResult("pInProgress");

    /**
     * Rule: IN_PROGRESS => block location is close to gps location
     */
    double pBlockLocation = library.computeBlockLocationProbability(
        parentState, blockState, obs);
    result.addResultAsAnd("pBlockLocation", pBlockLocation);
    
    /**
     * Rule: IN_PROGRESS => on route
     */
    CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
    ProjectedPoint p2 = obs.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
    double pOnRoute = _nearbyTripSigma.probability(d);
//    double pOnRoute = library.computeOnRouteProbability(state, obs);
    result.addResultAsAnd("pOnRoute", pOnRoute);

    return result;
  }

}
