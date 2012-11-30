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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DscLikelihood implements SensorModelRule {

  private DestinationSignCodeService _destinationSignCodeService;

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setRunService(RunService runService) {
  }

  public static enum DSC_STATE {
    DSC_OOS_IP, DSC_OOS_NOT_IP, DSC_IS_NO_BLOCK, DSC_MATCH, DSC_ROUTE_MATCH, DSC_NO_ROUTE_MATCH, DSC_NOT_VALID, DSC_DEADHEAD_MATCH
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {

    final SensorModelResult result = new SensorModelResult(
        "pDestinationSignCode");
    final DSC_STATE state = getDscState(context);
    switch (state) {
      case DSC_OOS_IP:
        result.addResultAsAnd("i.p. o.o.s. dsc", 0.0);
        return result;
      case DSC_NOT_VALID:
        result.addResultAsAnd("!valid dsc", 1d);
        return result;
      case DSC_OOS_NOT_IP:
        result.addResultAsAnd("!i.p. o.o.s. dsc", 1d);
        return result;
      case DSC_MATCH:
        result.addResultAsAnd("i.s. matching DSC", (13d/30d));
        return result;
      case DSC_DEADHEAD_MATCH:
        result.addResultAsAnd("i.s. deadhead matching DSC", (8d/30d));
        return result;
      case DSC_ROUTE_MATCH:
        result.addResultAsAnd("i.s. route-matching/deadhead-before/after", (8d/30d));
        return result;
      case DSC_IS_NO_BLOCK:
        result.addResultAsAnd("!o.o.s. dsc null-block", (1d/30d));
        return result;
      case DSC_NO_ROUTE_MATCH:
        result.addResultAsAnd("i.s. non-route-matching DSC", 0.0);
        return result;
      default:
        return null;
    }

  }

  public DSC_STATE getDscState(final Context context) {
    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();

    final JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();

    final String observedDsc = obs.getLastValidDestinationSignCode();

    if (observedDsc == null || !obs.hasValidDsc() || obs.hasOutOfServiceDsc()) {
      /**
       * If we haven't yet seen a valid DSC, or it's out of service
       */
      if (!obs.hasValidDsc()) {
        return DSC_STATE.DSC_NOT_VALID;
      } else if (EVehiclePhase.IN_PROGRESS == phase && obs.hasOutOfServiceDsc()) {
        return DSC_STATE.DSC_OOS_IP;
      } else {
        return DSC_STATE.DSC_OOS_NOT_IP;
      }
    } else {
      final BlockState bs = state.getBlockState();
      if (bs == null) {
        return DSC_STATE.DSC_IS_NO_BLOCK;
      } else {
        final Set<String> dscs = Sets.newHashSet();
        dscs.add(bs.getDestinationSignCode());
        final Set<AgencyAndId> routes = Sets.newHashSet();
        routes.add(bs.getBlockLocation().getActiveTrip().getTrip().getRouteCollection().getId());

        /*
         * dsc changes occur between parts of a block, so account for that
         */
        if (EVehiclePhase.LAYOVER_DURING == phase
            || EVehiclePhase.DEADHEAD_DURING == phase) {
          final BlockTripEntry nextTrip = bs.getBlockLocation().getActiveTrip().getNextTrip();

          if (nextTrip != null) {
            final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(nextTrip.getTrip().getId());
            if (dsc != null)
              dscs.add(dsc);
            routes.add(nextTrip.getTrip().getRouteCollection().getId());
          }
        }

        /*
         * Check if it's active, since deadhead-after/before's
         * can/should have o.o.s. dsc's, although they often don't.
         * TODO perhaps check the last non-o.o.s. dsc to give
         * higher weight to deadhead-after's that match (when
         * we have good run-info, perhaps).
         */
        if (!EVehiclePhase.isActiveAfterBlock(phase) && !EVehiclePhase.isActiveBeforeBlock(phase)
            && dscs.contains(observedDsc)) {
          if (EVehiclePhase.IN_PROGRESS == phase)
            return DSC_STATE.DSC_MATCH;
          else
            return DSC_STATE.DSC_DEADHEAD_MATCH;
        } else {
          /*
           * a dsc implies a route. even though the reported dsc may not match,
           * we expect the general route to be the same...
           */
          boolean routeMatch = false;
          for (final AgencyAndId dscRoute : obs.getDscImpliedRouteCollections()) {
            if (routes.contains(dscRoute)) {
              routeMatch = true;
              break;
            }
          }

          if (routeMatch)
            return DSC_STATE.DSC_ROUTE_MATCH;
          else
            return DSC_STATE.DSC_NO_ROUTE_MATCH;
        }

      }
    }
  }

}
