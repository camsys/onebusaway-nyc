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
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.print.attribute.standard.PDLOverrideSupported;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;

@Component
public class EdgeLikelihood implements SensorModelRule {

//  private DeviationModel _nearbyTripSigma = new DeviationModel(50.0);
  private final FoldedNormalDist _gpsDist = new FoldedNormalDist(20.0, 50.0);
  private final NormalDist _schedDevDist = new NormalDist(10.0, 30.0);

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

    /*
     * When we have no block-state, we use the best state with a 
     * route that matches the observations DSC-implied route, if any.
     */
    if (blockState == null
        || phase == EVehiclePhase.DEADHEAD_BEFORE) {
      BestBlockStates bestStates = _blockStateService.getBestBlockStateForRoute(obs);
      if (bestStates != null)
        blockState = bestStates.getBestLocation();
    }
    
    SensorModelResult result = new SensorModelResult("pInProgress");
    
    if (!(phase == EVehiclePhase.IN_PROGRESS
        || phase == EVehiclePhase.DEADHEAD_BEFORE 
        || phase == EVehiclePhase.DEADHEAD_DURING)
        || blockState == null)
      return result;
    
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
      
      if ((pBlockState == null 
            && obs.getPreviousObservation() != null)
          || (pBlockState != null 
              &&!pBlockState.getBlockInstance().equals(blockState.getBlockInstance()))
              ) {
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
      
      int expSchedTime = (int) (pBlockState.getBlockLocation().getScheduledTime()
          + (obs.getTime() - obs.getPreviousObservation().getTime())/1000);
      BlockState expState = _blockStateService.getScheduledTimeAsState(blockState.getBlockInstance(), expSchedTime); 
      
      expectedDabDelta = expState.getBlockLocation().getDistanceAlongBlock()
          - prevDab;
      
//      pMovement = NormalDist.density(expectedDabDelta, 10.0, actualDabDelta);
    } else {
      expectedDabDelta = 0.0;
      actualDabDelta = 0.0;
    }
    

    double pInProgress = FastMath.log(1.0 - FoldedNormalDist.cdf(10.0, 50.0, d)) 
        + FastMath.log(1.0 - FoldedNormalDist.cdf(5.0, 40.0, obsSchedDev)) 
        + FastMath.log(1.0 - NormalDist.cdf(expectedDabDelta, 10.0, actualDabDelta));
//    double pInProgress = FastMath.log(pGpsDist) + FastMath.log(pSchedDev) + FastMath.log(pMovement);
    
    result.addResult("gps", 1.0 - FoldedNormalDist.cdf(10.0, 50.0, d));
    result.addResult("schedule", 1.0 - FoldedNormalDist.cdf(5.0, 40.0, obsSchedDev));
    result.addResult("distance", 1.0 - NormalDist.cdf(expectedDabDelta, 10.0, actualDabDelta));
      
    if (phase != EVehiclePhase.IN_PROGRESS) {
      double invResult = 1.0 - FastMath.exp(pInProgress);
      result.addResultAsAnd("pNotInProgress", invResult);
    } else {
      result.addResultAsAnd("pInProgress", FastMath.exp(pInProgress));
    }

    return result;
  }

}
