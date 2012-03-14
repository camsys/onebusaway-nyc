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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

public interface BlockStateSamplingStrategy {

  /****
   *
   ****/

  public CategoricalDist<BlockStateObservation> cdfForJourneyAtStart(
      Observation observation);

  public CategoricalDist<BlockStateObservation> cdfForJourneyInProgress(
      Observation observation);

  public double scoreState(BlockStateObservation state,
      Observation observation, boolean atStart);

  public BlockStateObservation sampleTransitionDistanceState(BlockStateObservation parentBlockStateObs,
      Observation obs, boolean vehicleNotMoved, EVehiclePhase eVehiclePhase);

  public BlockStateObservation sampleTransitionScheduleDev(
      BlockStateObservation parentBlockStateObs, Observation obs);

  public BlockStateObservation sampleGpsObservationState(
      BlockStateObservation parentBlockStateObs, Observation obs);

  public BlockStateObservation samplePriorScheduleState(BlockInstance blockInstance,
      Observation obs);
}