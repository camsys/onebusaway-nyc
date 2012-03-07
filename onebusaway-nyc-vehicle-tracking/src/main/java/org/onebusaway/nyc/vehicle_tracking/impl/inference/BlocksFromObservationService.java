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
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

import java.util.Set;

public interface BlocksFromObservationService {

  public void determinePotentialBlocksForObservation(Observation observation,
      Set<BlockInstance> potentialBlocks);

  public Set<BlockStateObservation> bestStates(Observation observation,
      BlockStateObservation blockState);

  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation, boolean bestBlockLocation);

  public BlockStateObservation advanceLayoverState(Observation obs,
      BlockStateObservation blockState);

  public Set<BlockStateObservation> advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel);

  public Set<BlockStateObservation> determinePotentialBlockStatesForObservation(
      Observation observation);

  public BlockStateObservation getBlockStateObservation(Observation obs,
      BlockInstance blockInstance, double distanceAlong);

  public Set<BlockStateObservation> getSnappedBlockStates(Observation obs);

  public BlockStateObservation getBlockStateObservationFromTime(
      Observation obs, BlockInstance blockInstance, int newSchedTime);
}