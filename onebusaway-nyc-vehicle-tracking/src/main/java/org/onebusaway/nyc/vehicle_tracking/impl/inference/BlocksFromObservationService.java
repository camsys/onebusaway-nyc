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

import java.util.Set;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

public interface BlocksFromObservationService {

  public Set<BlockInstance> determinePotentialBlocksForObservation(
      Observation observation, Set<BlockInstance> nearbyBlocks);

  public Set<BlockState> advanceState(Observation observation,
      BlockState blockState, double minDistanceToTravel,
      double maxDistanceToTravel);

  public BlockState advanceLayoverState(long timestamp, BlockState blockState);

  public Set<BlockState> bestStates(Observation observation, BlockState blockState);


  public Set<BlockState> getReportedBlockStates(Observation observation,
      Set<BlockInstance> potentialBlocks, boolean bestBlockLocation,
      Set<BlockState> statesToUpdate);

  Set<BlockState> determinePotentialBlockStatesForObservation(
      Observation observation, boolean bestBlockLocation);
}