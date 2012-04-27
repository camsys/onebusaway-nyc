package org.onebusaway.nyc.transit_data_federation.services.schedule;

public interface ScheduledServiceService {

  public Boolean routeHasUpcomingScheduledService(long time, String routeId, String directionId);

  public Boolean stopHasUpcomingScheduledService(long time, String stopId, String routeId, String directionId);

}