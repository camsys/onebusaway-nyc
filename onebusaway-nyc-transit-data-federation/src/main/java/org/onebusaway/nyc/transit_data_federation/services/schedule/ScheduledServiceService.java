package org.onebusaway.nyc.transit_data_federation.services.schedule;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.schedule.ServiceHour;

public interface ScheduledServiceService {

  public Boolean routeHasUpcomingScheduledService(ServiceHour serviceHour, AgencyAndId routeId, String directionId);

  public Boolean stopHasUpcomingScheduledService(ServiceHour serviceHour, AgencyAndId stopId, AgencyAndId routeId, String directionId);

}