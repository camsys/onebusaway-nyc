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

import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;

@Component
class BlockStateSamplingStrategyImpl implements BlockStateSamplingStrategy {

  private ScheduledBlockLocationService _scheduledBlockLocationService;
  
  private BlocksFromObservationService _blocksFromObservationService;

  @Autowired
  public void setScheduledBlockService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Override
  public BlockStateObservation sampleTransitionDistanceState(
      BlockStateObservation parentBlockStateObs, Observation obs,
      boolean vehicleNotMoved, EVehiclePhase parentPhase) {

	double distAlongSample;
    final BlockState parentBlockState = parentBlockStateObs.getBlockState();
    final double parentDistAlong = parentBlockState.getBlockLocation().getDistanceAlongBlock();

    if (!vehicleNotMoved) {

      /*
       * Check if we're deadheading between trips.
       */
      if (EVehiclePhase.DEADHEAD_DURING == parentPhase 
          && !parentBlockStateObs.isOnTrip()) {
        /*
         * We use the observed distance moved in the direction of the next stop.
         */
        final BlockStopTimeEntry nextStop = parentBlockStateObs.getBlockState().getBlockLocation().getNextStop();

        if (nextStop == null
            || nextStop.getDistanceAlongBlock() <= parentDistAlong) {
          /*
           * When we're at the end of the block, sample movement as normal.
           * Basically, this should hit the distance-along limit and put us into
           * deadhead-after, or go the other way and keep us sitting around in
           * deadhead-during.  Either way, we want to allow both those possibilities.
           */
          final double distAlongPrior = obs.getDistanceMoved();
          final double distAlongErrorSample = ParticleFactoryImpl.getLocalRng().nextGaussian()
              * GpsLikelihood.gpsStdDev;
          distAlongSample = distAlongPrior
              + Math.max(distAlongErrorSample, -distAlongPrior);
        } else {

          final double prevDistToNextStop = SphericalGeometryLibrary.distance(
              obs.getPreviousObservation().getLocation(),
              nextStop.getStopTime().getStop().getStopLocation());
          final double currentDistToNextStop = SphericalGeometryLibrary.distance(
              obs.getLocation(),
              nextStop.getStopTime().getStop().getStopLocation());

          double distAlongPrior = prevDistToNextStop - currentDistToNextStop;

          if (distAlongPrior <= 0d)
            distAlongPrior = 0d;

          distAlongSample = distAlongPrior;
        }
      } else if (EVehiclePhase.DEADHEAD_AFTER == parentPhase
          || ((EVehiclePhase.DEADHEAD_BEFORE == parentPhase 
            || EVehiclePhase.LAYOVER_BEFORE == parentPhase) 
            && parentBlockStateObs.getScheduleDeviation() == 0d)) {
        /*
         * Only start moving if it's supposed to be
         */
        return new BlockStateObservation(parentBlockStateObs, obs);
      } else {
        final double distAlongPrior = obs.getDistanceMoved();
        final double distAlongErrorSample = ParticleFactoryImpl.getLocalRng().nextGaussian()
            * GpsLikelihood.gpsStdDev;
        distAlongSample = distAlongPrior
            + Math.max(distAlongErrorSample, -distAlongPrior);
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
    return orientationCheck(parentBlockStateObs, distState, obs);
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
    
    /*
     * Get the location at for the current time, then sample a location
     * based on that time and the travel time to that location (for when
     * we're not anywhere nearby).
     */
    final int startSchedTime = Iterables.getFirst(
        blockInstance.getBlock().getStopTimes(), null).getStopTime().getArrivalTime();
    final int endSchedTime = Iterables.getLast(
        blockInstance.getBlock().getStopTimes(), null).getStopTime().getDepartureTime();
    
    final double timeToGetToCurrentTimeLoc;
    if (currentTime > startSchedTime
        && currentTime < endSchedTime
        && obs.getPreviousObservation() != null) {
      final ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockInstance.getBlock(), (int)currentTime);
      
      /*
       * If the current time puts us in deadhead-during between trips, then
       * it's possible that the block location will be at the start
       * of the previous trip (potentially very far away), so we skip
       * these situations.
       */
      if (JourneyStateTransitionModel.isLocationOnATrip(blockLocation)) {
        final double impliedVelocity = obs.getDistanceMoved() / obs.getTimeDelta();
        timeToGetToCurrentTimeLoc = SphericalGeometryLibrary.distance(blockLocation.getLocation(),
            obs.getLocation()) / impliedVelocity;
      } else {
        timeToGetToCurrentTimeLoc = 0d;
      }
    } else {
      timeToGetToCurrentTimeLoc = 0d;
    }
  
    /*
     * TODO Note that we're using the non-run-matching prior distribution.
     * Should we?
     */
    final StudentTDistribution schedDist = ScheduleLikelihood.getSchedDevNonRunDist();
    final double schedTimeError = 60d * schedDist.sample(ParticleFactoryImpl.getLocalRng());
    double newSchedTime = currentTime + timeToGetToCurrentTimeLoc 
        + Math.max(schedTimeError, -timeToGetToCurrentTimeLoc/3d);
    
    if (Double.isInfinite(newSchedTime))
      return null;


    BlockStateObservation schedState;
    if (newSchedTime < startSchedTime) {
      schedState = _blocksFromObservationService.getBlockStateObservationFromDist(
          obs, blockInstance, 0.0);
    } else if (endSchedTime < newSchedTime) {
      return null;
    } else {
      /**
       * Important note about prior distribution sampling: to reduce/remove
       * confusion caused by deadhead states having no pre-defined trajectory,
       * we simply don't allow prior sampling of deadhead states for certain
       * situations.
       */
      schedState = _blocksFromObservationService.getBlockStateObservationFromTime(
          obs, blockInstance, (int) newSchedTime);
      if (!schedState.isOnTrip()) {
        return null;
      }
    }

    return orientationCheck(null, schedState, obs);
  }

  @Override
  public BlockStateObservation sampleTransitionScheduleDev(
      BlockStateObservation parentBlockStateObs, Observation obs) {
    final BlockState parentBlockState = parentBlockStateObs.getBlockState();

    final StudentTDistribution schedDist = ScheduleLikelihood.getSchedDistForBlockState(parentBlockStateObs);
    final double newSchedDev = schedDist.sample(ParticleFactoryImpl.getLocalRng());

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
      return null;
    } else {
      schedState = _blocksFromObservationService.getBlockStateObservationFromTime(
          obs, parentBlockState.getBlockInstance(), newSchedTime);
    }

    return orientationCheck(parentBlockStateObs, schedState, obs);
  }

  /**
   * Quick and dirty check for the direction of the trip.
   * 
   * @param blockStateObs
   * @param observation
   * @return
   */
  private BlockStateObservation orientationCheck(
      BlockStateObservation parentBlockStateObs,
      BlockStateObservation blockStateObs, Observation observation) {
    Double obsOrientation = null;
    Double distMoved = null;
    if (observation.getPreviousRecord() != null && blockStateObs.isOnTrip()) {
      distMoved = observation.getDistanceMoved();
      obsOrientation = observation.getOrientation();
      if (Double.isNaN(obsOrientation)) {
        obsOrientation = blockStateObs.getBlockState().getBlockLocation().getOrientation();
      }
      
      final double orientDiff = Math.abs(obsOrientation
          - blockStateObs.getBlockState().getBlockLocation().getOrientation());
      
      if (orientDiff >= 140d && orientDiff <= 225d
          && distMoved >= BlockStateService.getOppositeDirMoveCutoff()) {
        /*
         * If we weren't previously on a trip, but were going the wrong
         * direction, then truncate this sample up to the start of the trip.
         */
        if (parentBlockStateObs != null && !parentBlockStateObs.isOnTrip()) {
          final BlockState parentBlockState = parentBlockStateObs.getBlockState();
          final double adjustedDistAlong = blockStateObs.getBlockState().getBlockLocation().getActiveTrip().getDistanceAlongBlock();
          if (adjustedDistAlong > parentBlockState.getBlockLocation().getDistanceAlongBlock()
              && adjustedDistAlong < blockStateObs.getBlockState().getBlockLocation().getDistanceAlongBlock()) {
            final BlockStateObservation adjustedState = _blocksFromObservationService.getBlockStateObservationFromDist(
                observation, parentBlockState.getBlockInstance(),
                adjustedDistAlong);
            return adjustedState;
          }
          return null;
        } else {
          return null;
        }
      }
    }

    return blockStateObs;

  }

}
