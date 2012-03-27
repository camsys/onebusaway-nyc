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

import com.google.common.collect.Iterables;

import org.springframework.stereotype.Component;

import gov.sandia.cognition.statistics.distribution.UnivariateGaussian;

@Component
public class EdgeLikelihood implements SensorModelRule {

  final public static UnivariateGaussian inProgressEdgeMovementDist = new UnivariateGaussian(
      0d, 70d * 70d);
  final public static UnivariateGaussian deadDuringEdgeMovementDist = new UnivariateGaussian(
      0d, 150d * 150d);
  final public static UnivariateGaussian noEdgeMovementDist = new UnivariateGaussian(
      0d, 250d * 250d);

  // final public static double deadheadDuringStdDev = 150.0/1;

  // final public static double inProgressStdDev = 70.0/1;

  // final public static double startBlockStdDev = 50.0/1;

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

    if (obs.isAtBase()) {
      result.addResultAsAnd("pNotInProgress(base)", 1.0);
      return result;
    }

    if (blockState == null) {

      final double pDistAlong = computeNoEdgeMovementLogProb(obs);

      result.addLogResultAsAnd("pNotInProgress(no-edge)", pDistAlong);
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
      
      EVehiclePhase prevPhase = parentState.getJourneyState().getPhase();
      
      if (parentState.getBlockStateObservation().isSnapped()
          && state.getBlockStateObservation().isSnapped()) {
          pDistAlong = computeEdgeMovementLogProb(obs, state, parentState);
        
      } else {

        if (EVehiclePhase.IN_PROGRESS != prevPhase
            || EVehiclePhase.IN_PROGRESS != phase) {
          pDistAlong = computeNoEdgeMovementLogProb(obs);
        } else {
          pDistAlong = computeEdgeMovementLogProb(obs, state, parentState);
        }
//        if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
//  
//          pDistAlong = computeNoEdgeMovementLogProb(obs);
//  
//        } else if (EVehiclePhase.DEADHEAD_AFTER == phase) {
//  
//          if (parentState.getBlockState().getBlockLocation().getDistanceAlongBlock() 
//              < blockState.getBlockLocation().getDistanceAlongBlock()) {
//  
//            pDistAlong = computeEdgeMovementLogProb(obs, state, parentState);
//          } else {
//            pDistAlong = computeNoEdgeMovementLogProb(obs);
//          }
//          
//        } else if (EVehiclePhase.DEADHEAD_DURING == phase) {
//  
//          pDistAlong = computeNoEdgeMovementLogProb(obs);
//  
//        } else {
//  
//          pDistAlong = computeEdgeMovementLogProb(obs, state, parentState);
//  
//        }
      }
      result.addLogResultAsAnd("in-progress", pDistAlong);

    } else {

      if (EVehiclePhase.IN_PROGRESS == phase) {
        final double obsDelta = SphericalGeometryLibrary.distance(
            obs.getLocation(), obs.getPreviousObservation().getLocation());
        final double obsToTripDelta = SphericalGeometryLibrary.distance(
            blockState.getBlockLocation().getLocation(), obs.getPreviousObservation().getLocation());
        pDistAlong = inProgressEdgeMovementDist.getProbabilityFunction().logEvaluate(
            obsToTripDelta - obsDelta);
      } else {
        pDistAlong = computeNoEdgeMovementLogProb(obs);
      }

      result.addLogResultAsAnd("new-run", pDistAlong);

    }

    return result;
  }

  static private final double computeNoEdgeMovementLogProb(Observation obs) {
    final double obsDistDelta = SphericalGeometryLibrary.distance(
        obs.getLocation(), obs.getPreviousObservation().getLocation());
    final double obsTimeDelta = (obs.getTime() - obs.getPreviousObservation().getTime()) / 1000d;

    // FIXME really lame. use a Kalman filter.
    final double expAvgDist = 6.7056 * obsTimeDelta;

    final double pDistAlong = noEdgeMovementDist.getProbabilityFunction().logEvaluate(
        obsDistDelta - expAvgDist);
    return pDistAlong;
  }

  static private final double computeEdgeMovementLogProb(Observation obs, 
      VehicleState state, VehicleState parentState) {

    final double currentDab = state.getBlockState().getBlockLocation().getDistanceAlongBlock();
    final double prevDab = parentState.getBlockState().getBlockLocation().getDistanceAlongBlock();
    final double dabDelta = currentDab - prevDab;

    final double pMove;
    final EVehiclePhase phase = state.getJourneyState().getPhase();
//    if (EVehiclePhase.DEADHEAD_DURING == phase 
//        || EVehiclePhase.DEADHEAD_DURING == parentState.getJourneyState().getPhase()) {
//      
//      BlockStopTimeEntry nextStop = EVehiclePhase.DEADHEAD_DURING != phase ? 
//          parentState.getBlockState().getBlockLocation().getNextStop() :
//            state.getBlockState().getBlockLocation().getNextStop();
//      final double lastDelta = SphericalGeometryLibrary.distance(obs.getPreviousObservation().getLocation(),
//          nextStop.getStopTime().getStop().getStopLocation());
//      
//      final double currentDelta; 
//      if (nextStop.getDistanceAlongBlock() <= state.getBlockState().getBlockLocation().getDistanceAlongBlock()) {
//        currentDelta = -1 * (state.getBlockState().getBlockLocation().getDistanceAlongBlock() -
//            state.getBlockState().getBlockLocation().getActiveTrip().getDistanceAlongBlock());
//      } else {
//        currentDelta = SphericalGeometryLibrary.distance(obs.getLocation(),
//          nextStop.getStopTime().getStop().getStopLocation());
//      }
//      
//      final double obsDelta = lastDelta - currentDelta;
//      
//      pMove = deadDuringEdgeMovementDist.getProbabilityFunction().logEvaluate(
//          dabDelta - obsDelta);
//    } else {
      final double obsDelta = SphericalGeometryLibrary.distance(
          obs.getLocation(), obs.getPreviousObservation().getLocation());
  
      pMove = inProgressEdgeMovementDist.getProbabilityFunction().logEvaluate(
          dabDelta - obsDelta);
//    }

    return pMove;
  }
}
