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

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlockStateSamplingStrategyImpl implements BlockStateSamplingStrategy {

  private static Logger _log = LoggerFactory
      .getLogger(BlockStateSamplingStrategyImpl.class);

  private DestinationSignCodeService _destinationSignCodeService;

  /**
   * We need some way of scoring nearby trips
   */
  private DeviationModel _nearbyTripSigma = new DeviationModel(400.0);

  private DeviationModel _scheduleDeviationSigma = new DeviationModel(32 * 60);

  private BlocksFromObservationService _blocksFromObservationService;

  private ObservationCache _observationCache;


  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }
  
  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  /**
   * 
   * @param scheduleDeviationSigma
   *          time, in seconds
   */
  public void setScheduleDeviationSigma(int scheduleDeviationSigma) {
    _scheduleDeviationSigma = new DeviationModel(scheduleDeviationSigma);
  }

  @Override
  public CategoricalDist<BlockState> cdfForJourneyAtStart(Observation observation) {

    CategoricalDist<BlockState> cdf = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_START_BLOCK_CDF);

    if (cdf == null) {

      Set<BlockState> potentialBlocks = _blocksFromObservationService
            .determinePotentialBlockStatesForObservation(observation, false);

      cdf = new CategoricalDist<BlockState>();

      StringBuilder b = null;

      if (_log.isDebugEnabled()) {
        b = new StringBuilder();
        b.append("potential blocks found: ").append(potentialBlocks.size());
      }

      for (BlockState state: potentialBlocks) {

        double p = scoreJourneyStartState(state, observation);

        cdf.put(p, state);

        if (_log.isDebugEnabled()) {
          b.append("\n" + state.getBlockLocation().getDistanceAlongBlock()
              + "\t" + state.getBlockLocation().getScheduledTime() + "\t" + p
              + "\t" + state.getBlockInstance());

        }
      }

      if (_log.isDebugEnabled())
        _log.debug(b.toString());

      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.JOURNEY_START_BLOCK_CDF, cdf);
    }
    return cdf;
  }

  @Override
  public CategoricalDist<BlockState> cdfForJourneyInProgress(Observation observation) {

    CategoricalDist<BlockState> cdf = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK_CDF);

    if (cdf == null) {

      Set<BlockState> potentialBlocks = _blocksFromObservationService
          .determinePotentialBlockStatesForObservation(observation, true);

      cdf = new CategoricalDist<BlockState>();

      StringBuilder b = null;
      if (_log.isDebugEnabled()) {
        b = new StringBuilder();
        b.append("potential blocks found: " + potentialBlocks.size());
      }

      for (BlockState state: potentialBlocks) {

        double p = scoreState(state, observation);

        cdf.put(p, state);

        if (_log.isDebugEnabled()) {
          b.append("\n" + state.getBlockLocation().getDistanceAlongBlock()
              + "\t" + state.getBlockLocation().getScheduledTime() + "\t" + p
              + "\t" + state.getBlockInstance());
        }
      }

      if (_log.isDebugEnabled())
        _log.debug(b.toString());

      /**
       * Cache the result
       */
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK_CDF, cdf);
    }

    return cdf;
  }


  public double scoreState(BlockState state, Observation observation) {

    ScheduledBlockLocation blockLocation = state.getBlockLocation();
    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = observation.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
    double prob1 = _nearbyTripSigma.probability(d);

    BlockInstance blockInstance = state.getBlockInstance();
    long serviceDate = blockInstance.getServiceDate();
    int scheduledTime = blockLocation.getScheduledTime();

    long time = serviceDate + scheduledTime * 1000;
    long recordTime = observation.getTime();

    long timeDelta = Math.abs(time - recordTime) / 1000;
    double prob2 = _scheduleDeviationSigma.probability(timeDelta);

    return prob1 * prob2;
  }

  /****
   * Private Methods
   ****/

  private double scoreJourneyStartState(BlockState state,
      Observation observation) {
    return scoreJourneyStartDestinationSignCode(state, observation);
  }
  
  private double scoreJourneyStartDestinationSignCode(BlockState state,
      Observation observation) {

    String observedDsc = observation.getLastValidDestinationSignCode();

    boolean observedOutOfService = _destinationSignCodeService
        .isOutOfServiceDestinationSignCode(observedDsc);

    // If we have an out-of-service DSC, then we favor it equally
    if (observedOutOfService) {
      return 0.5;
    } else {
      // Favor blocks that match the correct DSC
      String dsc = state.getDestinationSignCode();
      if (StringUtils.equals(observedDsc, dsc))
        return 0.95;
      else
        return 0.25;
    }
  }
}
