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

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;

import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

import com.google.common.collect.Iterables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.randvar.NormalGen;

@Component
class BlockStateSamplingStrategyImpl implements BlockStateSamplingStrategy {

  private static Logger _log = LoggerFactory.getLogger(BlockStateSamplingStrategyImpl.class);

  /**
   * We need some way of scoring nearby trips
   */
  static public DeviationModel _nearbyTripSigma = new DeviationModel(400.0);

  static public DeviationModel _scheduleDeviationSigma = new DeviationModel(
      32 * 60);

  private BlocksFromObservationService _blocksFromObservationService;

  @Autowired
  public void setRunService(RunService runService) {
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
  }

  @Autowired
  public void setBlocksStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setOperatorAssignmentService(
      OperatorAssignmentService operatorAssignmentService) {
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
  }

  @Autowired
  public void setJourneyStateTransitionModel(
      JourneyStateTransitionModel journeyStateTransitionModel) {
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  static public void setScheduleDeviationSigma(int scheduleDeviationSigma) {
    _scheduleDeviationSigma = new DeviationModel(scheduleDeviationSigma);
  }

  @Override
  public BlockStateObservation sampleGpsObservationState(
      BlockStateObservation parentBlockStateObs, Observation obs) {

    double distAlongSample;
    final BlockState parentBlockState = parentBlockStateObs.getBlockState();
    final double parentDistAlong = parentBlockState.getBlockLocation().getDistanceAlongBlock();
    distAlongSample = NormalGen.nextDouble(
        ParticleFactoryImpl.getThreadLocalRng().get(), parentDistAlong,
        GpsLikelihood.gpsStdDev / 2);

    if (distAlongSample > parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance()) {
//      distAlongSample = parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance();
      return null;
    } else if (distAlongSample < 0.0) {
      distAlongSample = 0.0;
    }

    final BlockStateObservation distState = _blocksFromObservationService.getBlockStateObservationFromDist(
        obs, parentBlockState.getBlockInstance(), distAlongSample);
    return distState;

  }

  @Override
  public BlockStateObservation sampleTransitionDistanceState(
      BlockStateObservation parentBlockStateObs, Observation obs,
      boolean vehicleNotMoved, EVehiclePhase phase) {
    double distAlongSample;
    final BlockState parentBlockState = parentBlockStateObs.getBlockState();
    final double parentDistAlong = parentBlockState.getBlockLocation().getDistanceAlongBlock();
    
    if (!vehicleNotMoved) {
      
      if (EVehiclePhase.DEADHEAD_DURING == phase) {
        /*
         * We use the observed distance moved in the direction of the next stop.
         */
        BlockStopTimeEntry nextStop = parentBlockStateObs.getBlockState().getBlockLocation().getNextStop();
        
        if (nextStop == null || nextStop.getDistanceAlongBlock() <= parentDistAlong) {
          distAlongSample = 0d;
        } else {
        
          final double prevDistToNextStop = SphericalGeometryLibrary.distance(
              obs.getPreviousObservation().getLocation(), nextStop.getStopTime().getStop().getStopLocation());
          final double currentDistToNextStop = SphericalGeometryLibrary.distance(
              obs.getLocation(), nextStop.getStopTime().getStop().getStopLocation());
        
          double distAlongPrior = prevDistToNextStop - currentDistToNextStop;
          
          if (distAlongPrior <= 0d)
            distAlongPrior = 0d;
          
          distAlongSample = EdgeLikelihood.deadDuringEdgeMovementDist.sample(ParticleFactoryImpl.getLocalRng());
          distAlongSample += distAlongPrior;
        }
      } else {
        final double distAlongPrior = SphericalGeometryLibrary.distance(
            obs.getPreviousObservation().getLocation(), obs.getLocation());
        distAlongSample = EdgeLikelihood.inProgressEdgeMovementDist.sample(ParticleFactoryImpl.getLocalRng());
        distAlongSample += distAlongPrior;
      }
      
      distAlongSample += parentDistAlong;
    } else {
      return new BlockStateObservation(parentBlockStateObs, obs);
    }

    if (distAlongSample > parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance())
      distAlongSample = parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance();
    else if (distAlongSample < 0.0)
      distAlongSample = 0.0;

    final BlockStateObservation distState = _blocksFromObservationService.getBlockStateObservationFromDist(
        obs, parentBlockState.getBlockInstance(), distAlongSample);
    return distState;
  }

  @Override
  public BlockStateObservation samplePriorScheduleState(
      BlockInstance blockInstance, Observation obs) {

    /*
     * Our initial block proposals will yield 0 d.a.b. in some cases. It could
     * be that there is no snapped position for a block, yet it isn't actually
     * deadheading-before, it could be deadheading-during. That is why we sample
     * schedule deviations around the current obs time when the obs time is
     * after the block's start.
     */
    final double currentTime = (obs.getTime() - blockInstance.getServiceDate()) / 1000;

    final double newSchedTime = currentTime + 60d * ScheduleLikelihood.schedDevPriorDist.sample(
        ParticleFactoryImpl.getLocalRng());

    final int startSchedTime = Iterables.getFirst(
        blockInstance.getBlock().getStopTimes(), null).getStopTime().getArrivalTime();
    final int endSchedTime = Iterables.getLast(
        blockInstance.getBlock().getStopTimes(), null).getStopTime().getDepartureTime();

    BlockStateObservation schedState;
    if (newSchedTime < startSchedTime) {
      schedState = _blocksFromObservationService.getBlockStateObservationFromDist(
          obs, blockInstance, 0.0);
    } else if (endSchedTime < newSchedTime) {
//      schedState = _blocksFromObservationService.getBlockStateObservationFromDist(
//          obs, blockInstance, blockInstance.getBlock().getTotalBlockDistance());
      schedState = null;
    } else {
      schedState = _blocksFromObservationService.getBlockStateObservationFromTime(
          obs, blockInstance, (int)newSchedTime);
    }

    return schedState;
  }

  @Override
  public BlockStateObservation sampleTransitionScheduleDev(
      BlockStateObservation parentBlockStateObs, Observation obs) {
    final BlockState parentBlockState = parentBlockStateObs.getBlockState();

    final double newSchedDev = ScheduleLikelihood.schedDevPriorDist.sample(
        ParticleFactoryImpl.getLocalRng());

    final int currentTime = (int) (obs.getTime() - parentBlockState.getBlockInstance().getServiceDate()) / 1000;
    final int newSchedTime = currentTime - (int) (newSchedDev * 60.0);

    final int startSchedTime = Iterables.getFirst(
        parentBlockState.getBlockInstance().getBlock().getStopTimes(), null).getStopTime().getArrivalTime();
    final int endSchedTime = Iterables.getLast(
        parentBlockState.getBlockInstance().getBlock().getStopTimes(), null).getStopTime().getDepartureTime();
    BlockStateObservation schedState;
    if (newSchedTime < startSchedTime) {
      schedState = _blocksFromObservationService.getBlockStateObservationFromDist(
          obs, parentBlockState.getBlockInstance(), 0.0);
    } else if (endSchedTime < newSchedTime) {
//      schedState = _blocksFromObservationService.getBlockStateObservationFromDist(
//          obs,
//          parentBlockState.getBlockInstance(),
//          parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance());
      schedState = null;
    } else {
      schedState = _blocksFromObservationService.getBlockStateObservationFromTime(
          obs, parentBlockState.getBlockInstance(), newSchedTime);
    }

    return schedState;
  }

}
