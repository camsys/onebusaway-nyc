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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.util.List;

/**
 * We make this class immutable so that we don't have to worry about particle
 * filter methods changing it out from under us
 * 
 * @author bdferris
 * 
 */
public final class VehicleState implements Comparable<VehicleState> {

  private final MotionState motionState;

  private final BlockStateObservation blockStateObservation;

  private final JourneyState journeyState;

  private final List<JourneyPhaseSummary> journeySummaries;

  private final Observation observation;

  public VehicleState(VehicleState state) {
    this.motionState = state.motionState;
    this.blockStateObservation = state.blockStateObservation;
    this.journeyState = state.journeyState;
    this.journeySummaries = state.journeySummaries;
    this.observation = state.observation;
  }

  public VehicleState(MotionState motionState,
      BlockStateObservation blockState, JourneyState journeyState,
      List<JourneyPhaseSummary> journeySummaries, Observation observation) {
    this.motionState = Preconditions.checkNotNull(motionState);
    this.journeyState = Preconditions.checkNotNull(journeyState);
    this.observation = Preconditions.checkNotNull(observation);
    this.blockStateObservation = blockState;
    this.journeySummaries = journeySummaries;
  }

  public MotionState getMotionState() {
    return motionState;
  }

  public BlockStateObservation getBlockStateObservation() {
    return blockStateObservation;
  }

  public BlockState getBlockState() {
    return blockStateObservation != null
        ? blockStateObservation.getBlockState() : null;
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
    return Objects.toStringHelper("VehicleState").add("journeyState",
        journeyState).addValue(blockStateObservation).toString();
  }

  /**
   * This compareTo method is for definite ordering in CategoricalDist; such
   * ordering allows for reproducibility in testing.
   */
  @Override
  public int compareTo(VehicleState rightState) {

    if (this == rightState)
      return 0;

    final int compRes = ComparisonChain.start().compare(
        this.journeyState.getPhase(), rightState.getJourneyState().getPhase()).compare(
        this.blockStateObservation, rightState.getBlockStateObservation(),
        Ordering.natural().nullsLast()).compare(this.motionState,
        rightState.getMotionState()).compare(this.observation,
        rightState.getObservation()).result();

    return compRes;
  }

  private int _hash = 0;

  @Override
  public int hashCode() {
    if (_hash != 0)
      return _hash;

    final int prime = 31;
    int result = 1;
    result = prime
        * result
        + ((blockStateObservation == null) ? 0
            : blockStateObservation.hashCode());
    result = prime * result
        + ((journeyState == null) ? 0 : journeyState.getPhase().hashCode());
    result = prime * result
        + ((motionState == null) ? 0 : motionState.hashCode());
    result = prime * result
        + ((observation == null) ? 0 : observation.hashCode());
    _hash = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof VehicleState))
      return false;
    
    final VehicleState other = (VehicleState) obj;
    if (blockStateObservation == null) {
      if (other.blockStateObservation != null)
        return false;
    } else if (!blockStateObservation.equals(other.blockStateObservation))
      return false;
    
    if (journeyState == null) {
      if (other.journeyState != null)
        return false;
    } else if (other.journeyState == null) {
      return false;
    } else if (!journeyState.getPhase().equals(other.journeyState.getPhase()))
      return false;

    if (motionState == null) {
      if (other.motionState != null)
        return false;
    } else if (!motionState.equals(other.motionState))
      return false;
    if (observation == null) {
      if (other.observation != null)
        return false;
    } else if (!observation.equals(other.observation))
      return false;

    return true;
  }

}
