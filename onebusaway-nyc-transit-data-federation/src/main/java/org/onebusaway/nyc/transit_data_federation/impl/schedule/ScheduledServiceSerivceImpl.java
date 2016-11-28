package org.onebusaway.nyc.transit_data_federation.impl.schedule;

import java.util.Date;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.schedule.ScheduledServiceService;
import org.onebusaway.transit_data_federation.model.StopTimeInstance;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.StopTimeService.EFrequencyStopTimeBehavior;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduledServiceSerivceImpl implements ScheduledServiceService {
  
  private static final long SCHEDULE_WINDOW_BEFORE = 15 * 60 * 1000;

  private static final long SCHEDULE_WINDOW_AFTER = 60 * 60 * 1000;

  @Autowired
  private BlockCalendarService _blockCalendarService;
  
  @Autowired
  private BlockIndexService _blockIndexService;

  @Autowired
  private StopTimeService _stopTimeService;

  @Autowired
  private TransitGraphDao _transitGraphDao;

  @Override
  public Boolean routeHasUpcomingScheduledService(long time, String _routeId, String directionId) {    
    long serviceStart = time - SCHEDULE_WINDOW_BEFORE;
    long serviceEnd = time + SCHEDULE_WINDOW_AFTER;

    AgencyAndId routeId = AgencyAndIdLibrary.convertFromString(_routeId);
    
    List<BlockInstance> instances = 
        _blockCalendarService.getActiveBlocksForRouteInTimeRange(routeId, serviceStart, serviceEnd);
    
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

  @Override
  public Boolean stopHasUpcomingScheduledService(long time, String _stopId, String _routeId, String directionId) {
	AgencyAndId routeId = AgencyAndIdLibrary.convertFromString(_routeId);
	AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(_stopId);
	  
	StopEntry stopEntry = _transitGraphDao.getStopEntryForId(stopId);
    if (stopEntry == null) {
      return null;
    }
    
    Date serviceStart = new Date(time - SCHEDULE_WINDOW_BEFORE);
    Date serviceEnd = new Date(time + SCHEDULE_WINDOW_AFTER);

    List<StopTimeInstance> stis = _stopTimeService.getStopTimeInstancesInTimeRange(
        stopEntry, serviceStart, serviceEnd,
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

  @Override
  public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId,
          String routeId, String directionId) {
      AgencyAndId stop = AgencyAndIdLibrary.convertFromString(stopId);
      StopEntry stopEntry = _transitGraphDao.getStopEntryForId(stop);            
      List<BlockStopTimeIndex> stopTimeIndicesForStop = _blockIndexService.getStopTimeIndicesForStop(stopEntry);
      
      for (BlockStopTimeIndex bsti: stopTimeIndicesForStop) {
          List<BlockStopTimeEntry> stopTimes = bsti.getStopTimes();
          for (BlockStopTimeEntry bste: stopTimes) {
              StopTimeEntry stopTime = bste.getStopTime();
              
              TripEntry theTrip = stopTime.getTrip();
              
              if (routeId != null && !theTrip.getRoute().getId().equals(AgencyAndIdLibrary.convertFromString(routeId))) {
                  continue;
              }
              
              if (directionId != null && !theTrip.getDirectionId().equals(directionId)) {
                  continue;
              }
              
              /*
               * If at least one stoptime on one trip (subject to the
               * route and direction filters above) permits unrestricted
               * pick-up or drop-off at this stop (type=0), then it is
               * considered a 'revenue' stop.
               */
              if (stopTime.getDropOffType() == 0 ||
                      stopTime.getPickupType() == 0) {
                  return true;
              }
          }
      }
      
      return false;
  }

  @Override
  public Boolean stopHasRevenueService(String agencyId, String stopId) {
      return this.stopHasRevenueServiceOnRoute(agencyId, stopId,
              null, null);
  }
}
