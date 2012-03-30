package org.onebusaway.nyc.transit_data_federation.impl.schedule;

import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.schedule.ServiceHour;
import org.onebusaway.nyc.transit_data_federation.services.schedule.ScheduledServiceService;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.StopTimeService.EFrequencyStopTimeBehavior;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class ScheduledServiceSerivceImpl implements ScheduledServiceService {
  
  @Autowired
  private BlockCalendarService _blockCalendarService;

  @Autowired
  private StopTimeService _stopTimeService;

  @Autowired
  private TransitGraphDao _transitGraphDao;

  @Cacheable
  @Override
  public Boolean routeHasUpcomingScheduledService(ServiceHour serviceHour, AgencyAndId routeId, String directionId) {    
    long serviceHourStart = serviceHour.getTime();
    long serviceHourEnd = serviceHourStart + (60 * 60 * 1000);

    List<BlockInstance> instances = 
        _blockCalendarService.getActiveBlocksForRouteInTimeRange(routeId, serviceHourStart, serviceHourEnd);
    
    if(instances.isEmpty()) {
      return false;
    }

    for(BlockInstance instance : instances) {
      List<BlockTripEntry> tripsInBlock = instance.getBlock().getTrips();
      if(tripsInBlock.isEmpty()) {
        continue;
      }
      
      for(BlockTripEntry blockTripEntry : tripsInBlock) {
        TripEntry tripEntry = blockTripEntry.getTrip();

        if(tripEntry.getRoute().getId().equals(routeId) && tripEntry.getDirectionId().equals(directionId)) {
          return true;
        }
      }
    }
    
    return false;
  }

  @Cacheable
  @Override
  public Boolean stopHasUpcomingScheduledService(ServiceHour serviceHour, AgencyAndId stopId, AgencyAndId routeId, String directionId) {
    StopEntry stopEntry = _transitGraphDao.getStopEntryForId(stopId);
    if (stopEntry == null) {
      return null;
    }
    
    Date serviceHourStart = new Date(serviceHour.getTime());
    Date serviceHourEnd = new Date(serviceHourStart.getTime() + (60 * 60 * 1000));

    List<StopTimeInstance> stis = _stopTimeService.getStopTimeInstancesInTimeRange(
        stopEntry, serviceHourStart, serviceHourEnd,
        EFrequencyStopTimeBehavior.INCLUDE_UNSPECIFIED);

    if(stis.isEmpty()) {
      return false;
    }
    
    for(StopTimeInstance stopTime : stis) {
      BlockTripEntry blockTripEntry = stopTime.getTrip();
      TripEntry tripEntry = blockTripEntry.getTrip();

      if(tripEntry.getRoute().getId().equals(routeId) && tripEntry.getDirectionId().equals(directionId)) {
        return true;
      }
    }

    return false;
  }
}
