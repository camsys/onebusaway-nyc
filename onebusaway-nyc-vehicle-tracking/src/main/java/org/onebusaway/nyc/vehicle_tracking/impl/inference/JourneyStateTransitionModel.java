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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JourneyStateTransitionModel {

  /*
   * Time that the vehicle needs to be not moving in order
   * to be a layover.
   */
  private static final double LAYOVER_WAIT_TIME = 240d;

  @Autowired
  public void setBlockStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
  }

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }
  
  public static boolean isDetour(Boolean currentStateIsSnapped,
      VehicleState parentState) {
    
    if (parentState == null
        || parentState.getBlockStateObservation() == null
        || currentStateIsSnapped != Boolean.FALSE)
      return false;
    
    /*
     * If it was previously snapped, in-service and 
     * in the middle of a block (true if this was called)
     * and the current state is not snapped, or it was in
     * detour and it's still not snapped, then it's a detour.
     */
    if (parentState.getJourneyState().isDetour()
        || (parentState.getBlockStateObservation().isSnapped() 
            && parentState.getBlockStateObservation().isOnTrip()
            && parentState.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS)
        ) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Determine the conditions necessary to say it's stopped at a layover.
   * @param vehicleNotMoved
   * @param timeDelta
   */
  public static boolean isLayoverStopped(boolean vehicleNotMoved, Double timeDelta) {
    if (timeDelta == null || !vehicleNotMoved)
      return false;
    
    if (timeDelta > LAYOVER_WAIT_TIME)
      return true;
    
    return false;
  }

  /*
   * A deterministic journey state logic.<br>
   */
  public JourneyState getJourneyState(BlockStateObservation blockState,
      VehicleState parentState, 
      Observation obs, boolean vehicleNotMoved) {
    
    if (_vehicleStateLibrary.isAtBase(obs.getLocation()))
      return JourneyState.atBase();
    
    final boolean isLayoverStopped = isLayoverStopped(vehicleNotMoved, obs.getTimeDelta());
      
    if (blockState != null) {
      final double distanceAlong = blockState.getBlockState().getBlockLocation().getDistanceAlongBlock();
      if (distanceAlong <= 0.0) {
        if (isLayoverStopped 
            && blockState.isAtPotentialLayoverSpot()) {
          return JourneyState.layoverBefore();
        } else {
          return JourneyState.deadheadBefore(null);
        }
      } else if (distanceAlong >= blockState.getBlockState().getBlockInstance().getBlock().getTotalBlockDistance()) {
        return JourneyState.deadheadAfter();
      } else {
        /*
         * In the middle of a block.
         */
        if (isLayoverStopped && blockState.isAtPotentialLayoverSpot()) {
          return JourneyState.layoverDuring();
        } else {
          if (blockState.isOnTrip()) {
            final boolean isDetour = isDetour(blockState.isSnapped(), parentState);
            if (isDetour)
              return JourneyState.detour();
            else if (obs.hasOutOfServiceDsc())
              return JourneyState.deadheadDuring(null);
            else
              return JourneyState.inProgress();
          } else {
            return JourneyState.deadheadDuring(null);
          }
        }
      }
    } else {
      if (isLayoverStopped && obs.isAtTerminal())
        return JourneyState.layoverBefore();
      else
        return JourneyState.deadheadBefore(null);
    }
  }

  static public boolean isLocationOnATrip(BlockState blockState) {
    final double distanceAlong = blockState.getBlockLocation().getDistanceAlongBlock();
    final BlockTripEntry trip = blockState.getBlockLocation().getActiveTrip();
    final double tripDistFrom = trip.getDistanceAlongBlock();
    final double tripDistTo = tripDistFrom
        + trip.getTrip().getTotalTripDistance();

    if (tripDistFrom < distanceAlong && distanceAlong <= tripDistTo)
      return true;
    else
      return false;
  }

  static public boolean isLocationActive(BlockState blockState) {
    final double distanceAlong = blockState.getBlockLocation().getDistanceAlongBlock();

    if (0.0 < distanceAlong
        && distanceAlong < blockState.getBlockInstance().getBlock().getTotalBlockDistance())
      return true;
    else
      return false;
  }
}
