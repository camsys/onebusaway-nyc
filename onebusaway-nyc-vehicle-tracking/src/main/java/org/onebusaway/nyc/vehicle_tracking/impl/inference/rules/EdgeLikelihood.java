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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;

import gov.sandia.cognition.statistics.distribution.UnivariateGaussian;

import org.springframework.stereotype.Component;

@Component
public class EdgeLikelihood implements SensorModelRule {

  // final public static UnivariateGaussian inProgressEdgeMovementDist = new
  // UnivariateGaussian(
  // 0d, 70d * 70d);

  // final public static UnivariateGaussian deadDuringEdgeMovementDist = new
  // UnivariateGaussian(
  // 0d, 150d * 150d);

  // final public static UnivariateGaussian noEdgeMovementDist = new
  // UnivariateGaussian(
  // 0d, 250d * 250d);
  private static final double _avgVelocity = 13.4112;
  private static final double _avgTripVelocity = 6.4112;

  // final public static double deadheadDuringStdDev = 150.0/1;

  // final public static double inProgressStdDev = 70.0/1;

  // final public static double startBlockStdDev = 50.0/1;

  // private BlockStateService _blockStateService;
  //
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
    EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();

    /*
     * TODO clean up this hack We are really in-progress, but because of the
     * out-of-service headsign, we can't report it as in-progress
     */
    if (obs.hasOutOfServiceDsc() && EVehiclePhase.DEADHEAD_DURING == phase
        && (blockStateObs != null && blockStateObs.isOnTrip()))
      phase = EVehiclePhase.IN_PROGRESS;

    final SensorModelResult result = new SensorModelResult("pEdge", 1.0);

    if (obs.getPreviousObservation() == null || parentState == null) {
      if (EVehiclePhase.isActiveDuringBlock(phase)) {
        result.addResultAsAnd("no prev. obs./vehicle-state(in-progress)", 1.0);
      } else {
        result.addResultAsAnd("no prev. obs./vehicle-state", 1.0);
      }
      return result;
    }

    if (obs.isAtBase()) {
      result.addResultAsAnd("pNotInProgress(base)", 1.0);
      return result;
    }

    if (blockStateObs == null) {

      final double pDistAlong = computeNoEdgeMovementLogProb(state, obs);

      result.addLogResultAsAnd("pNotInProgress(no-edge)", pDistAlong);
      return result;
    }

    /*
     * Edge Movement
     */
    final boolean previouslyInactive = parentState.getBlockState() == null;
    final boolean newRun = MotionModelImpl.hasRunChanged(
        parentState.getBlockStateObservation(),
        state.getBlockStateObservation());

    final boolean hasMoved = state.getMotionState().hasVehicleNotMoved();
    final double pDistAlong;
    if (!previouslyInactive && !newRun) {

      if (parentState.getBlockStateObservation().isSnapped()
          && state.getBlockStateObservation().isSnapped()) {
        pDistAlong = computeEdgeMovementLogProb(obs, state.getBlockState(),
            parentState.getBlockState(), hasMoved);
        result.addLogResultAsAnd("snapped states", pDistAlong);

      } else {

        /*
         * This could be a transition from being on a trip geom to off, and vice
         * versa.
         */
        if (EVehiclePhase.IN_PROGRESS != phase) {
          pDistAlong = computeNoEdgeMovementLogProb(state, obs);
          result.addLogResultAsAnd("not-in-progress", pDistAlong);
        } else if (EVehiclePhase.IN_PROGRESS != parentState.getJourneyState().getPhase()) {
          final BlockState previousTripState = parentState.getBlockState(); // library.getPreviousStateOnSameBlock(state);
          if (previousTripState != null)
            pDistAlong = computeEdgeMovementLogProb(obs, state.getBlockState(),
                previousTripState, hasMoved);
          else
            pDistAlong = computeEdgeEntranceMovementLogProb(state, obs);
          result.addLogResultAsAnd("just-in-progress", pDistAlong);
        } else {
          pDistAlong = computeEdgeMovementLogProb(obs, state.getBlockState(),
              parentState.getBlockState(), hasMoved);
          result.addLogResultAsAnd("in-progress", pDistAlong);
        }
      }

    } else {

      if (EVehiclePhase.IN_PROGRESS == phase) {
        final BlockState previousTripState = library.getPreviousStateOnSameBlock(state,
            state.getBlockState().getBlockLocation().getDistanceAlongBlock());
        if (previousTripState != null)
          pDistAlong = computeEdgeMovementLogProb(obs, state.getBlockState(),
              previousTripState, hasMoved);
        else
          pDistAlong = computeEdgeEntranceMovementLogProb(state, obs);
      } else {
        pDistAlong = computeNoEdgeMovementLogProb(state, obs);
      }

      result.addLogResultAsAnd("new-run", pDistAlong);

    }

    return result;
  }

  static private final double computeEdgeEntranceMovementLogProb(
      VehicleState state, Observation obs) {
    final double obsDelta = SphericalGeometryLibrary.distance(
        obs.getLocation(), obs.getPreviousObservation().getLocation());
    final double obsToTripDelta = SphericalGeometryLibrary.distance(
        state.getBlockState().getBlockLocation().getLocation(),
        obs.getPreviousObservation().getLocation());
    final double x = obsToTripDelta - obsDelta;
    final double obsTimeDelta = (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000d;
    return UnivariateGaussian.PDF.logEvaluate(x, 0d,
        Math.pow(obsTimeDelta, 4) / 4d);
    // return
    // inProgressEdgeMovementDist.getProbabilityFunction().logEvaluate(x);
  }

  static private final double computeNoEdgeMovementLogProb(VehicleState state,
      Observation obs) {

    final Observation prevObs = obs.getPreviousObservation();
    final double obsDistDelta = SphericalGeometryLibrary.distance(
        obs.getLocation(), obs.getPreviousObservation().getLocation());
    final double expAvgDist;
    final double pDistAlong;
    if (prevObs != null) {
      final double obsTimeDelta = (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000d;
      final Observation prevPrevObs = prevObs.getPreviousObservation();

      /*
       * Trying to get away with not using a real tracking filter FIXME really
       * lame. use a Kalman filter.
       */
      if (prevPrevObs != null) {
        final double prevObsDistDelta = SphericalGeometryLibrary.distance(
            prevPrevObs.getLocation(), prevObs.getLocation());
        final double prevObsTimeDelta = (prevObs.getTime() - prevPrevObs.getTime()) / 1000d;
        final double velocityEstimate = prevObsDistDelta / prevObsTimeDelta;
        expAvgDist = state.getMotionState().hasVehicleNotMoved() ? 0.0
            : velocityEstimate * obsTimeDelta;
      } else {
        expAvgDist = state.getMotionState().hasVehicleNotMoved() ? 0.0
            : _avgVelocity * obsTimeDelta;
      }

      // pDistAlong = noEdgeMovementDist.getProbabilityFunction().logEvaluate(
      // obsDistDelta - expAvgDist);
      pDistAlong = UnivariateGaussian.PDF.logEvaluate(obsDistDelta, expAvgDist,
          Math.pow(obsTimeDelta, 4) / 4d);

    } else {
      /*
       * No movement
       */
      pDistAlong = 1d;
    }

    return pDistAlong;
  }

  static public final double getAvgVelocityBetweenStops(BlockState blockState) {
    final BlockStopTimeEntry nextStop = blockState.getBlockLocation().getNextStop();
    if (nextStop != null && nextStop.getBlockSequence() - 1 > 0) {
      final BlockStopTimeEntry prevStop = blockState.getBlockInstance().getBlock().getStopTimes().get(
          nextStop.getBlockSequence() - 1);
      final double avgVelocity = (nextStop.getDistanceAlongBlock() - prevStop.getDistanceAlongBlock())
          / (nextStop.getStopTime().getArrivalTime() - prevStop.getStopTime().getDepartureTime());
      return avgVelocity;
    } else {
      return _avgTripVelocity;
    }

  }

  private static final double computeEdgeMovementLogProb(Observation obs,
      BlockState blockState, BlockState parentBlockState, boolean hasMoved) {

    final double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();
    final double prevDab = parentBlockState.getBlockLocation().getDistanceAlongBlock();
    final double dabDeltaTmp = currentDab - prevDab;
    final double dabDelta = dabDeltaTmp;

    // FIXME really lame. use a Kalman filter.
    final double obsTimeDelta = (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000d;
    final double expAvgDist;
    if (!hasMoved) {
      expAvgDist = getAvgVelocityBetweenStops(blockState) * obsTimeDelta;
    } else {
      expAvgDist = 0.0;
    }

    // final double obsDelta = SphericalGeometryLibrary.distance(
    // obs.getLocation(), obs.getPreviousObservation().getLocation());
    // final double x = dabDelta - obsDelta;
    final double x = dabDelta - expAvgDist;
    // final double pMove =
    // inProgressEdgeMovementDist.getProbabilityFunction().logEvaluate(x);
    final double pMove = UnivariateGaussian.PDF.logEvaluate(x, 0d,
        Math.pow(obsTimeDelta, 4) / 4d);

    return pMove;
  }
}
