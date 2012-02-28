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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class JourneyStateTransitionModel {

  private final JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

  private BlockStateTransitionModel _blockStateTransitionModel;

  @Autowired
  public void setBlockStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
    _blockStateTransitionModel = blockStateTransitionModel;
  }

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  /****
   * 
   * 
   ****/

  public void move(VehicleState parentState, MotionState motionState,
      Observation obs, Collection<VehicleState> vehicleStates) {

    final List<JourneyState> journeyStates = getTransitionJourneyStates(
        parentState, obs, motionState);

    generateTransitionVehicleStates(parentState, motionState, journeyStates, obs,
        vehicleStates);
  }

  public List<JourneyState> getTransitionJourneyStates(
      VehicleState parentState, Observation obs, MotionState motionState) {

    if (parentState == null)
      return movePrior(obs, motionState);
    
    final JourneyState parentJourneyState = parentState.getJourneyState();

    /*
     * short-circuit on at-base when we definitely haven't moved and
     * are within a base geom. 
     */
    boolean vehicleDefinitelyNotMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 1.0;
    if (vehicleDefinitelyNotMoved && obs.isAtBase()) {
      return Arrays.asList(JourneyState.atBase());
    }
    
    switch (parentJourneyState.getPhase()) {
      case AT_BASE:
        return moveAtBase(parentState, obs, motionState);
      case DEADHEAD_BEFORE:
        return moveDeadheadBefore(parentState, obs, parentJourneyState, motionState);
      case LAYOVER_BEFORE:
        return moveLayoverBefore(parentState, obs, motionState);
      case IN_PROGRESS:
        return moveInProgress(parentState, obs, motionState);
      case DEADHEAD_DURING:
        return moveDeadheadDuring(parentState, obs, parentJourneyState, motionState);
      case LAYOVER_DURING:
        return moveLayoverDuring(parentState, obs, motionState);
      default:
        throw new IllegalStateException("unknown journey state: "
            + parentJourneyState.getPhase());
    }
  }


  /**
   * This takes all the possible journeyStates that the parentState could
   * transition to, given the observation, and populates results with the
   * resulting VehicleStates of each of those transitions.
   * <br>
   * NOTE: This can return an empty set, meaning that no transitions are
   * possible.
   * 
   * @param parentState
   * @param motionState
   * @param journeyStates
   * @param obs
   * @param results
   */
  private void generateTransitionVehicleStates(VehicleState parentState,
      MotionState motionState, List<JourneyState> journeyStates,
      Observation obs, Collection<VehicleState> results) {

    for (final JourneyState journeyState : journeyStates) {

      final Set<BlockStateObservation> blockStates = _blockStateTransitionModel.transitionBlockState(
          parentState, motionState, journeyState, obs);

      if (blockStates == null) {
        @SuppressWarnings("unused")
        List<JourneyPhaseSummary> summaries = null;
        if (ParticleFilter.getDebugEnabled()) {
          summaries = _journeyStatePhaseLibrary.extendSummaries(parentState,
              null, journeyState, obs);
        }

        final VehicleState vehicleState = new VehicleState(motionState, null,
            journeyState, null, obs);

        results.add(vehicleState);

      } else {
        for (final BlockStateObservation bs : blockStates) {

          @SuppressWarnings("unused")
          List<JourneyPhaseSummary> summaries = null;
          if (ParticleFilter.getDebugEnabled()) {
            summaries = _journeyStatePhaseLibrary.extendSummaries(parentState,
                bs, journeyState, obs);
          }

          final VehicleState vehicleState = new VehicleState(motionState, bs,
              journeyState, null, obs);

          results.add(vehicleState);
        }
      }
    }
  }

  private List<JourneyState> movePrior(Observation obs, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    res.addAll(Arrays.asList(JourneyState.deadheadBefore(obs.getLocation())));
    if (_vehicleStateLibrary.isAtBase(obs.getLocation()))
      res.add(JourneyState.atBase());
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    return res;
  }
  
  private List<JourneyState> moveAtBase(VehicleState parentState, Observation obs, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    res.addAll(Arrays.asList(JourneyState.deadheadBefore(obs.getLocation())));
    includeConditionalStates(parentState, res, obs);
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    return res;
  }

  private List<JourneyState> moveDeadheadBefore(VehicleState parentState,
      Observation obs, JourneyState parentJourneyState, MotionState motionState) {
    final JourneyStartState start = parentJourneyState.getData();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    final List<JourneyState> res = new ArrayList<JourneyState>();
    res.addAll(Arrays.asList(JourneyState.deadheadBefore(start.getJourneyStart())));
    includeConditionalStates(parentState, res, obs);
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    return res;
  }

  private List<JourneyState> moveLayoverBefore(VehicleState parentState, Observation obs, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    res.addAll(Arrays.asList(JourneyState.deadheadBefore(obs.getLocation())));
    includeConditionalStates(parentState, res, obs);
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    return res;
  }

  private List<JourneyState> moveInProgress(VehicleState parentState, Observation obs, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    res.addAll(Arrays.asList(
        JourneyState.deadheadBefore(obs.getLocation())));
    includeConditionalStates(parentState, res, obs);
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    includeDuring(res, parentState, vehicleDefinitelyMoved, obs);
    return res;
  }

  private List<JourneyState> moveDeadheadDuring(VehicleState parentState,
      Observation obs, JourneyState parentJourneyState, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    res.addAll(Arrays.asList(
        JourneyState.deadheadBefore(obs.getLocation())));
    includeConditionalStates(parentState, res, obs);
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    includeDuring(res, parentState, vehicleDefinitelyMoved, obs);
    return res;
  }

  private List<JourneyState> moveLayoverDuring(VehicleState parentState, Observation obs, MotionState motionState) {
    final List<JourneyState> res = new ArrayList<JourneyState>();
    includeConditionalStates(parentState, res, obs);
    boolean vehicleDefinitelyMoved = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs) == 0.0;
    if (!vehicleDefinitelyMoved)
        res.add(JourneyState.layoverBefore());
    includeDuring(res, parentState, vehicleDefinitelyMoved, obs);
    return res;
  }
  
  private void includeDuring(List<JourneyState> res, VehicleState parentState,
      boolean vehicleDefinitelyMoved, Observation obs) {
    /*
     * Have to serve some part of a block to have any -during state
     */
    if (parentState != null && parentState.getBlockState() != null
        && SensorModelSupportLibrary.hasServedSomePartOfBlock(parentState.getBlockState())) {
      if (parentState.getBlockStateObservation().isAtPotentialLayoverSpot()) {
        res.add(JourneyState.deadheadDuring(obs.getLocation()));
        if (!vehicleDefinitelyMoved)
          res.add(JourneyState.layoverDuring());
      }
    }
  }
  
  /**
   * Determine if observation satisfies conditions necessary to transition to
   * certain states with non-zero probability.
   * @param parentState TODO
   * @param res
   * @param obs
   * @param obs2 
   */
  private void includeConditionalStates(VehicleState parentState, final List<JourneyState> res, Observation obs) {
    /*
     * Cannot reasonably transition to in-progress if there isn't a previous
     * observation with which we can determine the direction of travel.
     */
    if (parentState.getBlockStateObservation() != null
        && !obs.isOutOfService() 
        && obs.getPreviousObservation() != null
        && !obs.getPreviousObservation().getRecord().locationDataIsMissing())
      res.add(JourneyState.inProgress());

    /*
     * Has to be at-base to be at base, obviously.
     */
    if (_vehicleStateLibrary.isAtBase(obs.getLocation()))
      res.add(JourneyState.atBase());
    
  }


}
