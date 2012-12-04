package org.onebusaway.nyc.transit_data_federation.services.schedule;

/**
 * Service that returns the state of scheduled service for a given set of parameters (time, route, direction, etc.)
 * 
 * @author jmaki
 *
 */
public interface ScheduledServiceService {

  public Boolean routeHasUpcomingScheduledService(long time, String routeId, String directionId);

  public Boolean stopHasUpcomingScheduledService(long time, String stopId, String routeId, String directionId);

}