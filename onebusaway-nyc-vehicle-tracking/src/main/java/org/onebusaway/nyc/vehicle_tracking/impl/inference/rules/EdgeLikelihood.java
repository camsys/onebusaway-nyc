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

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;

// @Component
public class EdgeLikelihood implements SensorModelRule {

  final public static double deadheadDuringStdDev = 150.0/1;

  final public static double inProgressStdDev = 70.0/1;

  final public static double startBlockStdDev = 50.0/1;

  // private BlockStateService _blockStateService;

  // @Autowired
  // public void setBlockStateService(BlockStateService blockStateService) {
  // _blockStateService = blockStateService;
  // }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockState blockState = state.getBlockState();

    final SensorModelResult result = new SensorModelResult("pEdge", 1.0);

    if (obs.getPreviousObservation() == null || parentState == null) {
      if (EVehiclePhase.isActiveDuringBlock(phase)) {
        result.addResultAsAnd("no prev. obs./vehicle-state(in-progress)", 1.0);
      } else {
        result.addResultAsAnd("no prev. obs./vehicle-state", 1.0);
      }
      return result;
    }

    if (blockState == null) {
      if (obs.isAtBase()) {
        result.addResultAsAnd("pNotInProgress(base)", 1.0);
      } else {
        /*
         * FIXME should use average velocity/time from prev. obs
         */
        final double x = NormalDist.inverseF(0.0, startBlockStdDev, 0.01);
        final double pDistAlong = NormalDist.density(0.0, startBlockStdDev, x);
        result.addResultAsAnd("pNotInProgress(prior)", pDistAlong);
      }
      return result;
    }

    parentState.getBlockState();
    /*
     * Edge Movement
     */
    final boolean previouslyInactive = parentState.getBlockState() == null;
    final boolean newRun = MotionModelImpl.hasRunChanged(
        parentState.getBlockStateObservation(),
        state.getBlockStateObservation());

    final double pDistAlong;
    if (!previouslyInactive && !newRun) {

      if (EVehiclePhase.DEADHEAD_BEFORE == phase) {

        pDistAlong = FoldedNormalDist.density(0.0, startBlockStdDev,
            blockState.getBlockLocation().getDistanceAlongBlock());

      } else if (EVehiclePhase.DEADHEAD_AFTER == phase) {

        if (parentState.getBlockState().getBlockLocation().getDistanceAlongBlock() < blockState.getBlockLocation().getDistanceAlongBlock()) {
          // TODO do something special to account for excess distance after the
          // block?
          pDistAlong = computeEdgeMovementProb(blockState, obs,
              parentState.getBlockState(), phase);
        } else {
          /*
           * FIXME should use average velocity/time from prev. obs
           */
          final double x = NormalDist.inverseF(0.0, startBlockStdDev, 0.01);
          pDistAlong = NormalDist.density(0.0, startBlockStdDev, x);
        }

      } else {

        pDistAlong = computeEdgeMovementProb(blockState, obs,
            parentState.getBlockState(), phase);

      }
      result.addResultAsAnd("moveAlong", pDistAlong);

    } else if (previouslyInactive) {
      /*
       * FIXME should use average velocity/time from prev. obs
       */
      final double x = NormalDist.inverseF(0.0, startBlockStdDev, 0.01);
      pDistAlong = NormalDist.density(0.0, startBlockStdDev, x);
      result.addResultAsAnd("previouslyInactive", pDistAlong);

    } else if (newRun) {

      final double x = NormalDist.inverseF(0.0, startBlockStdDev, 0.01);
      pDistAlong = NormalDist.density(0.0, startBlockStdDev, x);
      result.addResultAsAnd("newBlock", pDistAlong);
    }

    return result;
  }

  static private final double computeEdgeMovementProb(BlockState blockState,
      Observation obs, BlockState prevBlockState, EVehiclePhase phase) {
    final double obsDelta = SphericalGeometryLibrary.distance(
        obs.getLocation(), obs.getPreviousObservation().getLocation());

    /*
     * When there are multiple (potential) previously in-progress block-states,
     * we need to average over them to determine
     */
    final double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();
    final double prevDab = prevBlockState.getBlockLocation().getDistanceAlongBlock();
    final double dabDelta = currentDab - prevDab;

    // /*
    // * Use whichever comes first: previous time incremented by observed change
    // * in time, or last stop departure time.
    // */
    // final int expSchedTime = Math.min(
    // (int) (prevBlockState.getBlockLocation().getScheduledTime()
    // + (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000),
    // Iterables.getLast(
    // prevBlockState.getBlockInstance().getBlock().getStopTimes()).getStopTime().getDepartureTime());
    //
    // // FIXME need to compensate for distances over the end of a trip...
    // final double distanceAlongBlock =
    // _blockStateService.getDistanceAlongBlock(
    // blockState.getBlockInstance().getBlock(),
    // prevBlockState.getBlockLocation().getStopTimeIndex(), expSchedTime);
    // final double expectedDabDelta = distanceAlongBlock - prevDab;
    final double stdDev;
    if (EVehiclePhase.DEADHEAD_DURING == phase) {
      stdDev = deadheadDuringStdDev;
    } else {
      stdDev = inProgressStdDev;
    }

    final double pInP3Tmp = NormalDist.density(dabDelta, stdDev, obsDelta);

    return pInP3Tmp;
  }
}
