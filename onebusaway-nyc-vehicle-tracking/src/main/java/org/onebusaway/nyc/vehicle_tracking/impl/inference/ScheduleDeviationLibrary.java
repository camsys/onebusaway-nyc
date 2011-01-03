package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.transit_data_federation.impl.blocks.ScheduledBlockLocationLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
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
    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation actualBlockLocation = blockState.getBlockLocation();

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

    int delta = ScheduledBlockLocationLibrary.computeTravelTimeBetweenLocations(
        actualBlockLocation, scheduledBlockLocation);

    return delta + extra;
  }

  private ScheduledBlockLocation getScheduledBlockLocation(
      BlockInstance blockInstance, Observation obs) {

    Map<BlockInstance, ScheduledBlockLocation> m = _observationCache.getValueForObservation(
        obs, EObservationCacheKey.SCHEDULED_BLOCK_LOCATION);

    if (m == null) {
      m = new HashMap<BlockInstance, ScheduledBlockLocation>();
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
        
        if( scheduledBlockLocation.getNextStop() == null) {
          scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
              blockConfig, t);
        }
      }

      m.put(blockInstance, scheduledBlockLocation);
    }

    return m.get(blockInstance);
  }
}
