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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.algorithm.Angle;

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
  private static final double _inProgressConcParam = 8d;
  private static final double _deadheadConcParam = 3d;
  private static final double _deadheadEntranceConcParam = 5d;
  private static final double _stoppedConcParam = 0.5d;
  
  private static final double _inProgressProb = 0.85d;
  private static final double _deadheadOffProb = 1d - _inProgressProb;

  @Override
  public SensorModelResult likelihood(Context context) throws BadProbabilityParticleFilterException {

    final VehicleState state = context.getState();
    final VehicleState parentState = context.getParentState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockStateObservation blockStateObs = state.getBlockStateObservation();

//    /*-
//     * TODO clean up this hack 
//     * We are really in-progress, but because of the
//     * out-of-service headsign, we can't report it as in-progress
//     */
//    if (obs.hasOutOfServiceDsc() && EVehiclePhase.DEADHEAD_DURING == phase
//        && (blockStateObs != null && blockStateObs.isOnTrip()))
//      phase = EVehiclePhase.IN_PROGRESS;

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

      result.addResultAsAnd(computeNoEdgeMovementLogProb(state, obs));
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
    if (!previouslyInactive && !newRun) {

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
              result.addResultAsAnd(computeEdgeMovementLogProb(obs, state,
                  parentState, hasMoved, true));
            } else {
              result.addResultAsAnd(computeNoEdgeMovementLogProb(state, parentState, obs));
            }
          } else {
            result.addResultAsAnd(computeNoEdgeMovementLogProb(state, obs));
          }
        } else if (EVehiclePhase.IN_PROGRESS != parentState.getJourneyState().getPhase()) {
          /*
           * We were previously not in-progress, and now we are.
           */
          if (EVehiclePhase.isActiveDuringBlock(parentState.getJourneyState().getPhase())) {
            result.addResultAsAnd(computeEdgeMovementLogProb(obs, state,
                parentState, hasMoved, true));
          } else {
            result.addResultAsAnd(computeNoEdgeMovementLogProb(state, parentState, obs));
          }
        } else {
          /*
           * We're in-progress and were before.
           */
          result.addResultAsAnd(computeEdgeMovementLogProb(obs, state,
              parentState, hasMoved, false));
        }

    } else {
      /*
       * We have a new run, or just got one after having nothing.
       */
      
      if (EVehiclePhase.IN_PROGRESS == phase) {
        if (EVehiclePhase.isActiveDuringBlock(parentState.getJourneyState().getPhase())) {
          result.addResultAsAnd(computeEdgeMovementLogProb(obs, state,
              parentState, hasMoved, false));
        } else {
          result.addResultAsAnd(computeNoEdgeMovementLogProb(state, parentState, obs));
        }
        
      } else {
        result.addResultAsAnd(computeNoEdgeMovementLogProb(state, obs));
      }
    }

    return result;
  }

  static private final SensorModelResult computeNoEdgeMovementLogProb(VehicleState state,
      Observation obs) throws BadProbabilityParticleFilterException {
    return computeNoEdgeMovementLogProb(state, null, obs);
  }
  
  static private final SensorModelResult computeNoEdgeMovementLogProb(VehicleState state,
      VehicleState prevState, Observation obs) throws BadProbabilityParticleFilterException {

    final SensorModelResult result = new SensorModelResult("no-edge");
    final Observation prevObs = obs.getPreviousObservation();
    final double obsDistDelta = obs.getDistanceMoved();
    
    if (ParticleFilter.getDebugEnabled())
      result.addResult("dist: " + obsDistDelta, 0d);
    
    final double expAvgDist;
    final double logpDistAlong;
    if (prevObs != null) {

      /*
       * Trying to get away with not using a real tracking filter. 
       * FIXME really lame. use a Kalman filter.
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
      
      if (ParticleFilter.getDebugEnabled())
        result.addResult("expAvgDist: " + expAvgDist, 0d);
      
      /*
       * TODO: Is this needed?
       */
      final boolean prevInProgress;
      double prevOrientation;
      if (prevState != null
          && prevState.getBlockState() != null
          && prevState.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS) {
        prevOrientation = prevState.getBlockState().getBlockLocation().getOrientation();
        prevInProgress = true;
        if (ParticleFilter.getDebugEnabled())
          result.addResult("prevOrient (state): " + prevOrientation, 0d);
      } else {
        prevOrientation = prevObs.getOrientation();
        prevInProgress = false;
        if (ParticleFilter.getDebugEnabled())
          result.addResult("prevOrient: " + prevOrientation, 0d);
      }
      
          
      final boolean isOff;
      final double currentOrientation;
      if (state.getBlockState() != null
          && state.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS) {
        /*
         * We're entering an edge in progress, so we want to compare the 
         * entrance orientation to the direction of the edge
         */
        isOff = false;
        currentOrientation = state.getBlockState().getBlockLocation().getOrientation();
        /*
         * We don't want to use the obs orientation when we're considered stopped
         * since gps error can reverse the orientation, making reverse 
         * trip transitions look reasonable.
         * Similarly, if we were on a trip, use that orientation.
         * TODO FIXME: we should have the orientation in the motion state, then
         * we wouldn't need to evaluate this logic here.
         */
        if (!state.getMotionState().hasVehicleNotMoved()) {
          if (!prevInProgress) {
            prevOrientation = obs.getOrientation();
          }
          
          if (ParticleFilter.getDebugEnabled())
            result.addResult("new prevOrient (state): " + prevOrientation, 0d);
        }
      } else {
        isOff = true;
        currentOrientation = obs.getOrientation();
      }
          
      if (ParticleFilter.getDebugEnabled())
        result.addResult("curOrient: " + currentOrientation, 0d);
          
      final double orientDiff;
      if (Double.isNaN(prevOrientation) || Double.isNaN(currentOrientation)) {
        orientDiff = 0d;
      } else {
        orientDiff = Angle.diff(Math.toRadians(prevOrientation), Math.toRadians(currentOrientation));
      }
      
      logpDistAlong = UnivariateGaussian.PDF.logEvaluate(obsDistDelta, expAvgDist, Math.pow(obsTimeDelta, 4) / 4d) 
        + (isOff ? _deadheadOffProb * logVonMisesPdf(orientDiff, _deadheadConcParam)
            : _inProgressProb * logVonMisesPdf(orientDiff, _deadheadEntranceConcParam));

    } else {
      /*
       * No movement
       */
      logpDistAlong = 1d;
    }
    
    result.addLogResultAsAnd("lik", logpDistAlong);

    return result;
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

  private static final SensorModelResult computeEdgeMovementLogProb(Observation obs,
      VehicleState state, VehicleState parentState, boolean hasMoved, boolean isDuring) throws BadProbabilityParticleFilterException {

    final BlockState blockState = state.getBlockState();
    final BlockState parentBlockState = parentState.getBlockState();
    final double currentDab;
    final double prevDab;
    if (!blockState.getBlockInstance().equals(parentBlockState.getBlockInstance())) {
      /*
       * Note: this isn't really working like we want it to, since there are
       * separate shapes that share the same segments of geometry.
       */
      if (!Objects.equal(parentBlockState.getBlockLocation().getActiveTrip().getTrip().getShapeId(),
               blockState.getBlockLocation().getActiveTrip().getTrip().getShapeId())) {
        return computeNoEdgeMovementLogProb(state, parentState, obs);
      }
      currentDab = blockState.getBlockLocation().getDistanceAlongBlock() - 
          blockState.getBlockLocation().getActiveTrip().getDistanceAlongBlock();
      prevDab = parentBlockState.getBlockLocation().getDistanceAlongBlock() - 
          parentBlockState.getBlockLocation().getActiveTrip().getDistanceAlongBlock();
    } else {
      currentDab = blockState.getBlockLocation().getDistanceAlongBlock(); 
      prevDab = parentBlockState.getBlockLocation().getDistanceAlongBlock(); 
    }
    
    final SensorModelResult result = new SensorModelResult("edge-move");
    final double dabDelta = currentDab - prevDab;
    if (ParticleFilter.getDebugEnabled())
      result.addResult("dist: " + dabDelta, 0d);

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
    if (ParticleFilter.getDebugEnabled())
      result.addResult("expAvgDist: " + expAvgDist, 0d);

    final double x = dabDelta - expAvgDist;
    
    /*
     * Now, we need to measure the state transition likelihood
     * wrt. heading.
     * For the in-progress -> in-progress transition, we need
     * to calculate the expected turn-rate/orientation between the two states.
     */
    double obsOrientation = Math.toRadians(obs.getOrientation());
    if (Double.isNaN(obsOrientation)) {
      obsOrientation = Math.toRadians(blockState.getBlockLocation().getOrientation());
    } 
    if (ParticleFilter.getDebugEnabled())
      result.addResult("obsOrient:" + obsOrientation, 0d);
      
    final double orientDiff;
    final double logpOrient;
    if (hasMoved) {
      if (!isDuring || blockState.getBlockLocation().getNextStop() == null) {
        /*
         * TODO
         * We could use a polar velocity model and the turn rate...
         */
        final double prevOrientation = Math.toRadians(parentBlockState.getBlockLocation().getOrientation());
        final double currentOrientation = Math.toRadians(blockState.getBlockLocation().getOrientation());
        final double angleDiff = Angle.diff(prevOrientation, currentOrientation);
        final double avgExpOrientation = Angle.normalizePositive(prevOrientation + 
            (Angle.getTurn(prevOrientation, currentOrientation) == Angle.CLOCKWISE ? -1d : 1d) * angleDiff/2d);
        orientDiff = Angle.diff(obsOrientation, avgExpOrientation);
        if (ParticleFilter.getDebugEnabled())
          result.addResult("expOrient: " + avgExpOrientation, 0d);
        logpOrient = _inProgressProb * logVonMisesPdf(orientDiff, _inProgressConcParam);
      } else {
        /*
         * The orientation is wrt. the next stop destination, i.e.
         * we want to check that we're heading toward it.
         */
        double pathOrDestOrientation = Math.toRadians(SphericalGeometryLibrary.getOrientation(
            obs.getLocation().getLat(), 
            obs.getLocation().getLon(),
            blockState.getBlockLocation().getNextStop().getStopTime().getStop().getStopLocation().getLat(),
            blockState.getBlockLocation().getNextStop().getStopTime().getStop().getStopLocation().getLon()
            ));
        if (Double.isNaN(pathOrDestOrientation)) {
          pathOrDestOrientation = obsOrientation;
        } 
        orientDiff = Angle.diff(obsOrientation, pathOrDestOrientation);
        if (ParticleFilter.getDebugEnabled())
          result.addResult("expOrient: " + pathOrDestOrientation, 0d);
        logpOrient = _inProgressProb * logVonMisesPdf(orientDiff, _deadheadEntranceConcParam);
      }
    } else {
//      /*
//       * What if we're switching trips while in the middle of doing one?
//       * We assume that it's unlikely to do a complete 180 when we're
//       * stopped.
//       */
//      if (parentState.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS
//          && state.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS) {
//        orientDiff = Math.toRadians(blockState.getBlockLocation().getOrientation() 
//            - parentBlockState.getBlockLocation().getOrientation());
//      } else {
        orientDiff = Angle.diff(obsOrientation, Math.toRadians(blockState.getBlockLocation().getOrientation()));
//      }
      if (ParticleFilter.getDebugEnabled())
        result.addResult("orientDiff (stopped):" + orientDiff, 0d);
      logpOrient = _inProgressProb * logVonMisesPdf(orientDiff, _stoppedConcParam);
    }
    
    final double logpMove = UnivariateGaussian.PDF.logEvaluate(x, 0d, Math.pow(obsTimeDelta, 4) / 4d) 
        + logpOrient;
    
    result.addLogResultAsAnd("lik", logpMove);

    return result;
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
