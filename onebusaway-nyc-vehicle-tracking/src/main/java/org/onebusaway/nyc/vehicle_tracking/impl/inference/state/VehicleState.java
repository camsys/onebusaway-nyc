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

import java.util.Comparator;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.realtime.api.EVehiclePhase;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

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
    if (motionState == null)
      throw new IllegalArgumentException("motionState cannot be null");
    if (journeyState == null)
      throw new IllegalArgumentException("journeyPhase cannot be null");
    if (observation == null)
      throw new IllegalArgumentException("observation cannot be null");
    this.motionState = motionState;
    this.blockStateObservation = blockState;
    this.journeyState = journeyState;
    this.journeySummaries = journeySummaries;
    this.observation = observation;
  }

  public MotionState getMotionState() {
    return motionState;
  }

  public BlockStateObservation getBlockStateObservation() {
    return blockStateObservation;
  }
  
  public BlockState getBlockState() {
    return blockStateObservation != null ? blockStateObservation.getBlockState() : null;
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
    return journeyState + " " + blockStateObservation + " " + observation;
  }

  /**
   *  This compareTo method is for definite ordering
   *  in CategoricalDist; such ordering allows for
   *  reproducibility in testing. 
   */
  @Override
  public int compareTo(VehicleState rightState) {
    
    if (this == rightState)
      return 0;
    
    int compRes = ComparisonChain.start()
        .compare(this.journeyState.getPhase(), rightState.getJourneyState().getPhase())
        .compare(this.motionState, rightState.getMotionState())
        .compare(this.observation, rightState.getObservation())
        .compare(this.blockStateObservation, rightState.getBlockStateObservation(), 
            Ordering.natural().nullsLast())
        .result();
    
    return compRes;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((blockStateObservation == null) ? 0 : blockStateObservation.hashCode());
    // XXX we're only concerned with phase here
    result = prime * result
        + ((journeyState == null) ? 0 : journeyState.getPhase().hashCode());
//    result = prime * result
//        + ((journeySummaries == null) ? 0 : journeySummaries.hashCode());
    result = prime * result
        + ((motionState == null) ? 0 : motionState.hashCode());
    result = prime * result
        + ((observation == null) ? 0 : observation.hashCode());
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
    VehicleState other = (VehicleState) obj;
    if (blockStateObservation == null) {
      if (other.blockStateObservation != null)
        return false;
    } else if (!blockStateObservation.equals(other.blockStateObservation))
      return false;
    // XXX we're only concerned with phase here
    if (journeyState == null) {
      if (other.journeyState != null)
        return false;
    } else if (other.journeyState == null) {
      return false;
    } else if (!journeyState.getPhase().equals(other.journeyState.getPhase()))
      return false;
//    if (journeySummaries == null) {
//      if (other.journeySummaries != null)
//        return false;
//    } else if (!journeySummaries.equals(other.journeySummaries))
//      return false;
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
