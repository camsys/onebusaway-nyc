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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DestinationSignCodeRule implements SensorModelRule {

  private DestinationSignCodeService _destinationSignCodeService;

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }
  
  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final JourneyState js = state.getJourneyState();
    final EVehiclePhase phase = js.getPhase();

    final String observedDsc = obs.getLastValidDestinationSignCode();

    /**
     * If we haven't yet seen a valid DSC
     */
    if (observedDsc == null)
      return new SensorModelResult("pDestinationSignCode: no DSC set", 1.0);

    final SensorModelResult result = new SensorModelResult(
        "pDestinationSignCode");

    /**
     * Rule: out-of-service DSC => ! IN_PROGRESS
     */

    final double p1 = implies(p(obs.isOutOfService()),
        p(phase != EVehiclePhase.IN_PROGRESS));

    result.addResultAsAnd("out-of-service DSC => ! IN_PROGRESS", p1);

    /**
     * If it is out of service then the following route-based considerations
     * can't be applied.
     */
    if (obs.isOutOfService())
      return result;

    final BlockState bs = state.getBlockState();
    if (bs != null) {
      Set<String> dscs = Sets.newHashSet();
      dscs.add(bs.getDestinationSignCode());
      Set<AgencyAndId> routes = Sets.newHashSet();
      routes.add(bs.getBlockLocation().getActiveTrip().getTrip().getRouteCollection().getId());
      /*
       * DSC changes occur between parts of a block, so
       * account for that
       */
      
      if (EVehiclePhase.LAYOVER_DURING == phase
          || EVehiclePhase.DEADHEAD_DURING == phase) {
        BlockTripEntry nextTrip = bs.getBlockLocation().getActiveTrip()
                  .getNextTrip();
        
        if (nextTrip != null) {
          final String dsc = _destinationSignCodeService
              .getDestinationSignCodeForTripId(nextTrip.getTrip().getId());
          if (dsc != null)
            dscs.add(dsc);
          routes.add(nextTrip.getTrip().getRouteCollection().getId());
        }
      }
        
      if (dscs.contains(observedDsc)) {
        result.addResultAsAnd("in-service matching DSC", 1.0);
      } else {
        /*
         * a dsc implies a route. even though the reported dsc may not match, we
         * expect the general route to be the same...
         */


        boolean routeMatch = false;
        for (final AgencyAndId dscRoute : obs.getDscImpliedRouteCollections()) {
          if (routes.contains(dscRoute)) {
            routeMatch = true;
            break;
          }
        }

        if (routeMatch)
          result.addResultAsAnd("in-service route-matching DSC", 0.9);
        else
          result.addResultAsAnd("in-service non-route-matching DSC", 1e-5);
      }
    } else {
      result.addResultAsAnd("in-service no DSC predicted", 1.0);
    }

    return result;
  }

}
