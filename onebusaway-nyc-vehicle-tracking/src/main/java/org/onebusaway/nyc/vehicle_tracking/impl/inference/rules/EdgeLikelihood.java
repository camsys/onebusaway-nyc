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

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import com.google.common.base.Preconditions;

import cern.jet.math.Bessel;

import gov.sandia.cognition.statistics.distribution.UnivariateGaussian;

import org.springframework.stereotype.Component;

@Component
public class EdgeLikelihood implements SensorModelRule {

  /*
   * Hackish values for deadhead movement
   */
  private static final double _avgVelocity = 13.4112d;
  private static final double _avgTripVelocity = 6.4112d;
  
  /*
   * Parameters for a Von Mises distribution concentration parameter
   */
  private static final double _inProgressConcParam = 1d;
  private static final double _deadheadConcParam = 2d;

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {

    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();

    /*-
     * TODO clean up this hack 
     * We are really in-progress, but because of the
     * out-of-service headsign, we can't report it as in-progress
     */
    if (obs.hasOutOfServiceDsc() && EVehiclePhase.DEADHEAD_DURING == phase
        && (blockStateObs != null && blockStateObs.isOnTrip()))
      phase = EVehiclePhase.IN_PROGRESS;

    final SensorModelResult result = new SensorModelResult("pEdge", 1d);

    if (obs.getPreviousObservation() == null || parentState == null) {
      if (EVehiclePhase.isActiveDuringBlock(phase)) {
        result.addResultAsAnd("no prev. obs./vehicle-state(in-progress)", 1d);
      } else {
        result.addResultAsAnd("no prev. obs./vehicle-state", 1d);
      }
      return result;
    }

    if (obs.isAtBase()) {
      result.addResultAsAnd("pNotInProgress(base)", 1d);
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

    final boolean hasMoved = !state.getMotionState().hasVehicleNotMoved();
    final double pDistAlong;
    if (!previouslyInactive && !newRun) {

      if (parentState.getBlockStateObservation().isSnapped()
          && state.getBlockStateObservation().isSnapped()) {
        /*
         * We're snapped and were.
         */
        pDistAlong = computeEdgeMovementLogProb(obs, state,
            parentState.getBlockState(), hasMoved, false);
        result.addLogResultAsAnd("snapped states", pDistAlong);

      } else {

        /*
         * This could be a transition from being on a trip geom to off, and vice
         * versa.
         */
        if (EVehiclePhase.IN_PROGRESS != phase) {
          /*
           * We're currently not in-progress
           */
          if (EVehiclePhase.isActiveDuringBlock(phase)) {
            if (state.getBlockState().getBlockInstance().equals(
                parentState.getBlockState().getBlockInstance())) {
              pDistAlong = computeEdgeMovementLogProb(obs, state,
                  parentState.getBlockState(), hasMoved, true);
              result.addLogResultAsAnd("not-in-progress (during)", pDistAlong);
            } else {
              pDistAlong = computeEdgeEntranceMovementLogProb(state, obs);
              result.addLogResultAsAnd("not-in-progress (during & new block)", pDistAlong);
            }
          } else {
            pDistAlong = computeNoEdgeMovementLogProb(state, obs);
            result.addLogResultAsAnd("not-in-progress (before)", pDistAlong);
          }
        } else if (EVehiclePhase.IN_PROGRESS != parentState.getJourneyState().getPhase()) {
          /*
           * We were previously not in-progress, and now we are.
           */
          if (parentState.getBlockStateObservation().isSnapped()) {
            pDistAlong = computeEdgeMovementLogProb(obs, state,
                parentState.getBlockState(), hasMoved, false);
            result.addLogResultAsAnd("in-progress (prev snapped)", pDistAlong);
          } else if (EVehiclePhase.DEADHEAD_DURING == parentState.getJourneyState().getPhase()) {
            pDistAlong = computeEdgeMovementLogProb(obs, state,
                parentState.getBlockState(), hasMoved, false);
            result.addLogResultAsAnd("in-progress (starting trip in block/run)", pDistAlong);
          } else {
            pDistAlong = computeEdgeEntranceMovementLogProb(state, obs);
            result.addLogResultAsAnd("in-progress (starting block/run)", pDistAlong);
          }
        } else {
          /*
           * We're in-progress and were before.
           */
          pDistAlong = computeEdgeMovementLogProb(obs, state,
              parentState.getBlockState(), hasMoved, false);
          result.addLogResultAsAnd("in-progress", pDistAlong);
        }
      }

    } else {
      /*
       * We have a new run, or just got one after having nothing.
       */
      
      if (EVehiclePhase.IN_PROGRESS == phase) {
        final BlockState previousState = parentState.getBlockState();
            //library.getPreviousStateOnSameBlock(state,
            //state.getBlockState().getBlockLocation().getDistanceAlongBlock());
        if (previousState != null) {
          pDistAlong = computeEdgeMovementLogProb(obs, state,
              previousState, hasMoved, false);
          result.addLogResultAsAnd("new-run (edge movement)", pDistAlong);
        } else {
          pDistAlong = computeEdgeEntranceMovementLogProb(state, obs);
          result.addLogResultAsAnd("new-run (edge entrance)", pDistAlong);
        }
        
      } else {
        pDistAlong = computeNoEdgeMovementLogProb(state, obs);
        result.addLogResultAsAnd("new-run (no edge)", pDistAlong);
      }
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
    
    CoordinatePoint lastLoc = state.getMotionState().getLastInMotionLocation();
    double obsOrientation = SphericalGeometryLibrary.getOrientation(
        lastLoc.getLat(), 
        lastLoc.getLon(),
        obs.getLocation().getLat(),
        obs.getLocation().getLon());
    if (Double.isNaN(obsOrientation)) {
      obsOrientation = state.getBlockState().getBlockLocation().getOrientation();
    } 
    
    final double orientDiff = Math.toRadians(obsOrientation - state.getBlockState().getBlockLocation().getOrientation());
      
    final double logLik = UnivariateGaussian.PDF.logEvaluate(x, 0d, Math.pow(obsTimeDelta, 4) / 4d)
          + logVonMisesPdf(orientDiff, EVehiclePhase.IN_PROGRESS !=  state.getJourneyState().getPhase()
          ? _deadheadConcParam : _inProgressConcParam);
    return logLik;
  }

  static private final double computeNoEdgeMovementLogProb(VehicleState state,
      Observation obs) {

    final Observation prevObs = obs.getPreviousObservation();
    final double obsDistDelta = SphericalGeometryLibrary.distance(
        obs.getLocation(), obs.getPreviousObservation().getLocation());
    final double expAvgDist;
    final double pDistAlong;
    if (prevObs != null) {

      /*
       * Trying to get away with not using a real tracking filter FIXME really
       * lame. use a Kalman filter.
       */
      final double obsTimeDelta = obs.getTimeDelta();
      if (prevObs.getTimeDelta() != null) {
        final double prevObsDistDelta = prevObs.getDistanceMoved();
        final double prevObsTimeDelta = prevObs.getTimeDelta();
        final double velocityEstimate = prevObsDistDelta / prevObsTimeDelta;
        expAvgDist = state.getMotionState().hasVehicleNotMoved() ? 0d
            : velocityEstimate * obsTimeDelta;
      } else {
        expAvgDist = state.getMotionState().hasVehicleNotMoved() ? 0d
            : _avgVelocity * obsTimeDelta;
      }
      
      double prevOrientation = prevObs.getOrientation(); 
      
      final double currentOrientation = obs.getOrientation(); 
      final double orientDiff;
      if (Double.isNaN(prevOrientation) || Double.isNaN(currentOrientation)) {
        orientDiff = 0d;
      } else {
        orientDiff = Math.toRadians(prevOrientation - currentOrientation);
      }
      
      pDistAlong = UnivariateGaussian.PDF.logEvaluate(obsDistDelta, expAvgDist, Math.pow(obsTimeDelta, 4) / 4d) 
        + logVonMisesPdf(orientDiff, _deadheadConcParam);

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
      VehicleState state, BlockState parentBlockState, boolean hasMoved, boolean isDuring) {

    final BlockState blockState = state.getBlockState();
    final double currentDab;
    final double prevDab;
    if (!blockState.getBlockInstance().equals(parentBlockState.getBlockInstance())) {
      Preconditions.checkState(!isDuring);
      /*
       * Note: this isn't really working like we want it to, since there are
       * separate shapes that share the same segments of geometry.
       */
      if (!parentBlockState.getBlockLocation().getActiveTrip().getTrip().getShapeId().equals(
               blockState.getBlockLocation().getActiveTrip().getTrip().getShapeId())) {
        return computeEdgeEntranceMovementLogProb(state, obs);
      }
      currentDab = blockState.getBlockLocation().getDistanceAlongBlock() - 
          blockState.getBlockLocation().getActiveTrip().getDistanceAlongBlock();
      prevDab = parentBlockState.getBlockLocation().getDistanceAlongBlock() - 
          parentBlockState.getBlockLocation().getActiveTrip().getDistanceAlongBlock();
    } else {
      currentDab = blockState.getBlockLocation().getDistanceAlongBlock(); 
      prevDab = parentBlockState.getBlockLocation().getDistanceAlongBlock(); 
    }
    
    final double dabDelta = currentDab - prevDab;

    // FIXME really lame. use a Kalman filter.
    final double obsTimeDelta = (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000d;
    final double expAvgDist;
    if (hasMoved) {
      if (!isDuring || blockState.getBlockLocation().getNextStop() == null){
        expAvgDist = getAvgVelocityBetweenStops(blockState) * obsTimeDelta;
      } else {
        expAvgDist = getAvgVelocityToNextStop(obs, blockState) * obsTimeDelta;
      }
    } else {
      expAvgDist = 0d;
    }

    final double x = dabDelta - expAvgDist;
    
    final double orientDiff;
    if (hasMoved) {
      double obsOrientation = obs.getOrientation();
      if (Double.isNaN(obsOrientation)) {
        obsOrientation = blockState.getBlockLocation().getOrientation();
      } 
      
      double pathOrDestOrientation;
      if (!isDuring || blockState.getBlockLocation().getNextStop() == null) {
        /*
         * Orientation is the direction of the trip.
         */
        pathOrDestOrientation = blockState.getBlockLocation().getOrientation();
      } else {
        /*
         * The orientation is wrt. the next stop destination, i.e.
         * we want to check that we're heading toward it.
         */
        pathOrDestOrientation = SphericalGeometryLibrary.getOrientation(
            obs.getLocation().getLat(), 
            obs.getLocation().getLon(),
            blockState.getBlockLocation().getNextStop().getStopTime().getStop().getStopLocation().getLat(),
            blockState.getBlockLocation().getNextStop().getStopTime().getStop().getStopLocation().getLon()
            );
        if (Double.isNaN(pathOrDestOrientation)) {
          pathOrDestOrientation = obsOrientation;
        } 
      }
      
      orientDiff = Math.toRadians(obsOrientation - pathOrDestOrientation);
    } else {
      orientDiff = 0d;
    }
    
    final double pMove = UnivariateGaussian.PDF.logEvaluate(x, 0d, Math.pow(obsTimeDelta, 4) / 4d) 
        + logVonMisesPdf(orientDiff, isDuring ? _inProgressConcParam : _deadheadConcParam);

    return pMove;
  }
  
  private static double getAvgVelocityToNextStop(Observation obs, BlockState blockState) {
    
    StopTimeEntry stop =  blockState.getBlockLocation().getNextStop().getStopTime();
    final double distToDest = SphericalGeometryLibrary.distance(obs.getLocation(),
         stop.getStop().getStopLocation());
    final int currSchedTime = (int)(obs.getTime() - blockState.getBlockInstance().getServiceDate())/1000;
    
    final double expectedVelocity;
    if (currSchedTime >= stop.getArrivalTime()) {
      /*
       * Assumption here is that the driver will speed up to get to the stop on time.
       * TODO: check this.
       */
      expectedVelocity = _avgVelocity * 3d/2d;
    } else {
      expectedVelocity = distToDest/(stop.getArrivalTime() - currSchedTime);
    }
    
    return expectedVelocity;
  }

  private static final double logVonMisesPdf(double x, double kappa) {
    return kappa * Math.cos(x) - Math.log(2d * Math.PI * Bessel.i0(kappa));
  }
}
