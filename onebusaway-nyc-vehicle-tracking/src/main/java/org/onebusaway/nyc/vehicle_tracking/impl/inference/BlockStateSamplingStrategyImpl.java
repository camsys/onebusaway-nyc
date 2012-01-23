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

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
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

  private static Logger _log = LoggerFactory.getLogger(BlockStateSamplingStrategyImpl.class);

  private DestinationSignCodeService _destinationSignCodeService;

  /**
   * We need some way of scoring nearby trips
   */
  static public DeviationModel _nearbyTripSigma = new DeviationModel(400.0);

  static public DeviationModel _scheduleDeviationSigma = new DeviationModel(
      32 * 60);

  private BlocksFromObservationService _blocksFromObservationService;

  private ObservationCache _observationCache;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

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
  public void setOperatorAssignmentService(
      OperatorAssignmentService operatorAssignmentService) {
    _operatorAssignmentService = operatorAssignmentService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  public void setScheduleDeviationSigma(int scheduleDeviationSigma) {
    _scheduleDeviationSigma = new DeviationModel(scheduleDeviationSigma);
  }

  @Override
  public CategoricalDist<BlockStateObservation> cdfForJourneyAtStart(
      Observation observation) {

    CategoricalDist<BlockStateObservation> cdf = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_START_BLOCK_CDF);

    if (cdf == null) {

      Set<BlockStateObservation> potentialBlocks = _blocksFromObservationService.determinePotentialBlockStatesForObservation(
          observation, false);

      cdf = new CategoricalDist<BlockStateObservation>();

      StringBuilder b = null;

      if (_log.isDebugEnabled()) {
        b = new StringBuilder();
        b.append("potential blocks found: ").append(potentialBlocks.size());
      }

      for (BlockStateObservation state : potentialBlocks) {

        double p = scoreState(state, observation, true);

        cdf.put(p, state);
      }

      if (_log.isDebugEnabled())
        _log.debug(b.toString());

      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.JOURNEY_START_BLOCK_CDF, cdf);
    }
    return cdf;
  }

  @Override
  public CategoricalDist<BlockStateObservation> cdfForJourneyInProgress(
      Observation observation) {

    CategoricalDist<BlockStateObservation> cdf = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.JOURNEY_IN_PROGRESS_BLOCK_CDF);

    if (cdf == null) {

      Set<BlockStateObservation> potentialBlocks = _blocksFromObservationService.determinePotentialBlockStatesForObservation(
          observation, true);

      cdf = new CategoricalDist<BlockStateObservation>();

      StringBuilder b = null;
      if (_log.isDebugEnabled()) {
        b = new StringBuilder();
        b.append("potential blocks found: " + potentialBlocks.size());
      }

      for (BlockStateObservation state : potentialBlocks) {

        double p = scoreState(state, observation, false);

        cdf.put(p, state);
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

  @Override
  public double scoreState(BlockStateObservation state,
      Observation observation, boolean atStart) {
    double score;

    if (atStart) {
      /**
       * If it's at start, we judge it by it's dsc/route
       */
      score = scoreJourneyStartState(state.getBlockState(), observation);
    } else {
      /**
       * If it's in-progress, we use sched. time and location deviances
       */
      ScheduledBlockLocation blockLocation = state.getBlockState().getBlockLocation();
      BlockInstance blockInstance = state.getBlockState().getBlockInstance();
      long serviceDate = blockInstance.getServiceDate();
      score = scoreJourneyInProgressState(blockLocation, observation,
          serviceDate);
      score *= scoreDestinationSignCode(state.getBlockState(), observation);
    }

    /**
     * In all cases we use the run info, when available, to determine a
     * preference
     */
    boolean operatorHasAssignment = false;
    try {
      operatorHasAssignment = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
          new ServiceDate(new Date(observation.getTime())),
          observation.getRecord().getOperatorId()) != null;

    } catch (Exception e) {
      _log.warn("Operator service was not available.");
    }

    Boolean noStateButRunInfo = state == null
        && (operatorHasAssignment || _runService.isValidRunNumber(observation.getRecord().getRunNumber()));

    Boolean stateButNoRunMatch = state != null
        && state.getOpAssigned() == Boolean.FALSE
        && state.getRunReported() == Boolean.FALSE;

    /**
     * Use only 10% of the score when a proposal doesn't use the run info
     * provided. Also, sample closer fuzzy matches.
     */
    if (noStateButRunInfo == Boolean.TRUE || stateButNoRunMatch == Boolean.TRUE) {
      score *= 0.10;
    } else if (state != null) {
      if (state.getRunReported() == Boolean.TRUE) {
        if (observation.getFuzzyMatchDistance() != null
            && observation.getFuzzyMatchDistance() > 0)
          score *= 0.95;
      }
    }

    return score;
  }

  public double scoreJourneyInProgressState(
      ScheduledBlockLocation blockLocation, Observation observation,
      long serviceDate) {

    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = observation.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
    double prob1 = _nearbyTripSigma.probability(d);

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
    return scoreDestinationSignCode(state, observation);
  }

  private double scoreDestinationSignCode(BlockState state,
      Observation observation) {

    String observedDsc = observation.getLastValidDestinationSignCode();

    boolean observedOutOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc);

    // If we have an out-of-service DSC, then we favor it equally
    if (observedOutOfService) {
      return 0.5;
    } else {
      // Favor in-service blocks that match the correct DSC
      String dsc = state.getDestinationSignCode();
      if (StringUtils.equals(observedDsc, dsc)) {
        return 1.0;
      } else {
        // Favor in-service blocks servicing the same route implied by the DSC
        Set<AgencyAndId> dscRoutes = _destinationSignCodeService.getRouteCollectionIdsForDestinationSignCode(dsc);
        AgencyAndId thisRoute = state.getBlockLocation().getActiveTrip().getTrip().getRouteCollection().getId();
        boolean sameRoute = false;
        for (AgencyAndId route : dscRoutes) {
          if (thisRoute.equals(route)) {
            sameRoute = true;
            break;
          }
        }

        if (sameRoute)
          return 0.85;
        else
          return 1e-5;
      }
    }
  }
}
