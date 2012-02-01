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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService.BestBlockStates;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MissingShapePointsException;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ScheduleDeviationLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

import com.google.common.collect.Iterables;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;

@Component
public class EdgeLikelihood implements SensorModelRule {

  private BlockStateService _blockStateService;
  private ScheduleDeviationLibrary _scheduleDeviationLibrary;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }
  
  @Autowired
  public void setScheduleDeviationLibrary(
      ScheduleDeviationLibrary scheduleDeviationLibrary) {
    _scheduleDeviationLibrary = scheduleDeviationLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    VehicleState parentState = context.getParentState();
    VehicleState state = context.getState();
    Observation obs = context.getObservation();

    JourneyState js = state.getJourneyState();
    EVehiclePhase phase = js.getPhase();
    BlockState blockState = state.getBlockState();
    
    SensorModelResult result = new SensorModelResult("pInProgress");
    
    if (!(phase == EVehiclePhase.IN_PROGRESS
        || phase == EVehiclePhase.DEADHEAD_BEFORE 
        || phase == EVehiclePhase.DEADHEAD_DURING))
      return result;

    /*
     * When we have no block-state, we use the best state with a 
     * route that matches the observations DSC-implied route, if any.
     */
    if (blockState == null
        || phase != EVehiclePhase.IN_PROGRESS) {
      BestBlockStates bestStates = _blockStateService.getBestBlockStateForRoute(obs);
      if (bestStates != null)
        blockState = bestStates.getBestLocation();
      else
        return result;
    }
    
    /*
     * GPS Error
     */
    CoordinatePoint p1 = blockState.getBlockLocation().getLocation();

    ProjectedPoint p2 = obs.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
//    double pGpsDist = _gpsDist.density(d);
    
    
    /*
     * Schedule Deviation
     */
    
    int obsSchedDev = _scheduleDeviationLibrary.computeScheduleDeviation(blockState.getBlockInstance(),
        blockState.getBlockLocation(), obs);
//    double pSchedDev = _schedDevDist.density(obsSchedDev);
    
    /*
     * Edge Movement
     */
    Double expectedDabDelta = null;
    Double actualDabDelta = null;
//    double pMovement = 0.0;
    if (parentState != null) {
      BlockState pBlockState = parentState.getBlockState();
      
      if ((pBlockState == null)
          || (pBlockState != null 
              && !pBlockState.getBlockInstance().equals(blockState.getBlockInstance()))) {
        if  (obs.getPreviousObservation() == null)
          return phase == EVehiclePhase.IN_PROGRESS ? result.addResultAsAnd("no previous observation", 0.0)
                                                    : result.addResultAsAnd("no previous observation", 1.0);
        try {
          pBlockState = _blockStateService.getBestBlockLocations(obs.getPreviousObservation(),
              blockState.getBlockInstance(), 0, Double.POSITIVE_INFINITY).getBestLocation();
        } catch (MissingShapePointsException e) {
          e.printStackTrace();
          return result.addResultAsAnd("missing shapepoints", 0.0);
        }
      } 
      
      double prevDab = pBlockState.getBlockLocation().getDistanceAlongBlock();
      double currentDab = blockState.getBlockLocation().getDistanceAlongBlock();
      
      actualDabDelta = currentDab - prevDab;
      
      /*
       * Use whichever comes first: previous time incremented by observed change in time,
       * or last stop departure time.
       */
      int expSchedTime = Math.min((int) (pBlockState.getBlockLocation().getScheduledTime()
          + (obs.getTime() - obs.getPreviousObservation().getTime())/1000), 
          Iterables.getLast(pBlockState.getBlockInstance().getBlock().getStopTimes()).getStopTime().getDepartureTime());
      
      // FIXME need to compensate for distances over the end of a trip...
      BlockState expState = _blockStateService.getScheduledTimeAsState(blockState.getBlockInstance(), expSchedTime); 
      
      expectedDabDelta = expState.getBlockLocation().getDistanceAlongBlock()
          - prevDab;
      
//      pMovement = NormalDist.density(expectedDabDelta, 10.0, actualDabDelta);
    } else {
      expectedDabDelta = 0.0;
      actualDabDelta = 0.0;
    }
    
    double distStdDev = 250.0;
//        Math.max(400,
//        SphericalGeometryLibrary.distance(obs.getLocation(),
//            obs.getPreviousObservation().getLocation()) * 1.5);
    double pInP1 = 1.0 - FoldedNormalDist.cdf(10.0, 50.0, d);
    double pInP2 = 1.0 - FoldedNormalDist.cdf(5.0, 40.0, obsSchedDev/60.0);
    double pInP3 = NormalDist.density(expectedDabDelta, distStdDev, actualDabDelta)/NormalDist.density(expectedDabDelta, distStdDev, expectedDabDelta);

    double pInProgress = FastMath.log(pInP1) + FastMath.log(pInP2) + FastMath.log(pInP3);
    
    result.addResult("gps", pInP1);
    result.addResult("schedule", pInP2);
    result.addResult("distance", pInP3);
      
    if (phase != EVehiclePhase.IN_PROGRESS) {
      double invResult = 1.0 - FastMath.exp(pInProgress);
      result.addResultAsAnd("pNotInProgress", invResult);
    } else {
      result.addResultAsAnd("pInProgress", FastMath.exp(pInProgress));
    }

    return result;
  }

}
