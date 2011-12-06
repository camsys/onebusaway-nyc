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

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DestinationSignCodeRule implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();

    String observedDsc = obs.getLastValidDestinationSignCode();

    /**
     * If we haven't yet seen a valid DSC
     */
    if (observedDsc == null)
      return new SensorModelResult("pDestinationSignCode: no DSC set", 1.0);

    SensorModelResult result = new SensorModelResult("pDestinationSignCode");

    /**
     * Rule: out-of-service DSC => ! IN_PROGRESS
     */

    double p1 = implies(p(obs.isOutOfService()),
        p(phase != EVehiclePhase.IN_PROGRESS));

    result.addResultAsAnd("out-of-service DSC => ! IN_PROGRESS", p1);
    
    /**
     * If it is out of service then the following route-based
     * considerations can't be applied.
     */
    if (obs.isOutOfService())
      return result;

    BlockState bs = state.getBlockState();
    if (bs != null) {
      /*
       * If we're in service and have a matching dsc, then we want to be 20%
       * more likely.
       */
      if (bs.getDestinationSignCode().equals(observedDsc)) {
        result.addResultAsAnd("in-service matching DSC", 1.0);
      } else {
        /*
         * a dsc implies a route. even though the reported dsc may not match,
         * we expect the general route to be the same...
         */

        AgencyAndId thisRoute = bs.getBlockLocation().getActiveTrip()
            .getTrip().getRouteCollection().getId();

        boolean routeMatch = false;
        for (AgencyAndId dscRoute : obs.getDscImpliedRouteCollections()) {
          if (thisRoute.equals(dscRoute)) {
            routeMatch = true;
            break;
          }
        }

        if (routeMatch)
          result.addResultAsAnd("in-service route-matching DSC", 0.9);
        else
          result.addResultAsAnd("in-service non-route-matching DSC", 0.01);
      }
    } else {
      result.addResultAsAnd("in-service no DSC predicted", 0.01);
    }

    return result;
  }

}
