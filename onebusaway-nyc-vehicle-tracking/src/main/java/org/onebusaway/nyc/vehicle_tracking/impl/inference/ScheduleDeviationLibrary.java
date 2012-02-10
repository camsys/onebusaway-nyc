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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.transit_data_federation.impl.blocks.ScheduledBlockLocationLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduleDeviationLibrary {

  private ObservationCache _observationCache;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  public int computeScheduleDeviation(VehicleState state,
      Observation observation) {

    BlockState blockState = state.getBlockState();
    ScheduledBlockLocation actualBlockLocation = blockState.getBlockLocation();
    return computeScheduleDeviation(blockState.getBlockInstance(),
        actualBlockLocation, observation);
  }

  public int computeScheduleDeviation(BlockInstance blockInstance,
      ScheduledBlockLocation actualBlockLocation, Observation observation) {

    int scheduleTime = (int) ((observation.getTime() - blockInstance.getServiceDate()) / 1000);

    ScheduledBlockLocation scheduledBlockLocation = getScheduledBlockLocation(
        blockInstance, observation);

    /**
     * If our effective schedule time is out beyond the end of the block, we
     * compute a block location at the end of the block. We want to penalize for
     * that extra time.
     */
    int extra = Math.max(0,
        scheduleTime - scheduledBlockLocation.getScheduledTime());

    int delta = computeTravelTimeBetweenLocations(
        actualBlockLocation, scheduledBlockLocation);

    return delta + extra;
  }

  private ScheduledBlockLocation getScheduledBlockLocation(
      BlockInstance blockInstance, Observation obs) {

    Map<BlockInstance, ScheduledBlockLocation> m = _observationCache.getValueForObservation(
        obs, EObservationCacheKey.SCHEDULED_BLOCK_LOCATION);

    if (m == null) {
      m = new ConcurrentHashMap<BlockInstance, ScheduledBlockLocation>();
      _observationCache.putValueForObservation(obs,
          EObservationCacheKey.SCHEDULED_BLOCK_LOCATION, m);
    }

    if (!m.containsKey(blockInstance)) {

      int scheduleTime = (int) ((obs.getTime() - blockInstance.getServiceDate()) / 1000);
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();
      ScheduledBlockLocation scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockConfig, scheduleTime);

      /**
       * Is our schedule time beyond the last stop in the block?
       */
      if (scheduledBlockLocation == null) {
        int n = blockConfig.getStopTimes().size();
        int t = blockConfig.getDepartureTimeForIndex(n - 1);
        scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
            blockConfig, t);

        if (scheduledBlockLocation.getNextStop() == null) {
          scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
              blockConfig, t);
        }
      }

      m.put(blockInstance, scheduledBlockLocation);
    }

    return m.get(blockInstance);
  }
  
  public static int computeTravelTimeBetweenLocations(
      ScheduledBlockLocation scheduledBlockLocationA,
      ScheduledBlockLocation scheduledBlockLocationB) {
    
    if( scheduledBlockLocationA.getScheduledTime() == scheduledBlockLocationB.getScheduledTime() )
      return 0;

    boolean inOrder = scheduledBlockLocationA.getScheduledTime() <= scheduledBlockLocationB.getScheduledTime();

    ScheduledBlockLocation from = inOrder ? scheduledBlockLocationA
        : scheduledBlockLocationB;
    ScheduledBlockLocation to = inOrder ? scheduledBlockLocationB
        : scheduledBlockLocationA;

    int delta = to.getScheduledTime() - from.getScheduledTime();

    BlockStopTimeEntry fromStop = from.getNextStop();
    BlockStopTimeEntry toStop = to.getNextStop();
    
    int slack = ((toStop != null) ? toStop.getAccumulatedSlackTime() : 0)
        - fromStop.getAccumulatedSlackTime();

    int slackFrom = computeSlackToNextStop(from);
    slack += slackFrom;
    if (toStop != null) {
      int slackTo = computeSlackToNextStop(to);
      slack -= slackTo;
    }

    return Math.max(0, delta - slack);
  }

  public static int computeSlackToNextStop(
      ScheduledBlockLocation scheduledBlockLocation) {

    BlockStopTimeEntry nextStop = scheduledBlockLocation.getNextStop();
    StopTimeEntry stopTime = nextStop.getStopTime();
    int t = scheduledBlockLocation.getScheduledTime();

    /**
     * If we are actually at the next stop already, we return a negative value:
     * the amount of slack time already consumed.
     */
    if (stopTime.getArrivalTime() <= t && t <= stopTime.getDepartureTime())
      return stopTime.getArrivalTime() - t;

    int sequence = nextStop.getBlockSequence();

    /**
     * Are we before the first stop in the block? Are we already at the stop or
     * on our way there? Not sure, for now, let's assume there is no slack to be
     * had
     */
    if (sequence == 0)
      return 0;

    BlockConfigurationEntry blockConfig = nextStop.getTrip().getBlockConfiguration();
    BlockStopTimeEntry previousStop = blockConfig.getStopTimes().get(
        sequence - 1);

    int slack = nextStop.getAccumulatedSlackTime()
        - previousStop.getAccumulatedSlackTime();
    slack -= previousStop.getStopTime().getSlackTime();

    int timeToNextStop = stopTime.getArrivalTime() - t;

    if (timeToNextStop > slack)
      return slack;
    else
      return timeToNextStop;
  }
}
