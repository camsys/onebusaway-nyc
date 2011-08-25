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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

/**
 * We make this class immutable so that we don't have to worry about particle
 * filter methods changing it out from under us
 * 
 * @author bdferris
 * 
 */
public final class VehicleState {

  private final MotionState motionState;

  private final BlockState blockState;

  private final JourneyState journeyState;

  private final List<JourneyPhaseSummary> journeySummaries;

  private final Observation observation;

  public VehicleState(VehicleState state) {
    this.motionState = state.motionState;
    this.blockState = state.blockState;
    this.journeyState = state.journeyState;
    this.journeySummaries = state.journeySummaries;
    this.observation = state.observation;
  }

  public VehicleState(MotionState motionState,
      BlockState blockState, JourneyState journeyState,
      List<JourneyPhaseSummary> journeySummaries, Observation observation) {
    if (motionState == null)
      throw new IllegalArgumentException("motionState cannot be null");
    if (journeyState == null)
      throw new IllegalArgumentException("journeyPhase cannot be null");
    if (observation == null)
      throw new IllegalArgumentException("observation cannot be null");
    this.motionState = motionState;
    this.blockState = blockState;
    this.journeyState = journeyState;
    this.journeySummaries = journeySummaries;
    this.observation = observation;
  }

  public MotionState getMotionState() {
    return motionState;
  }

  public BlockState getBlockState() {
    return blockState;
  }

  public JourneyState getJourneyState() {
    return journeyState;
  }

  public List<JourneyPhaseSummary> getJourneySummaries() {
    return journeySummaries;
  }

  public Observation getObservation() {
    return observation;
  }

  @Override
  public String toString() {
    return journeyState + " " + blockState + " " + observation;
  }
}
