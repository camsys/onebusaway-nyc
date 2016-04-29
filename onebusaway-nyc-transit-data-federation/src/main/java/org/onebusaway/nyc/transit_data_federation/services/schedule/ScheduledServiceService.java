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
  
  /**
   * Given a stop, route, and direction, test if that stop has revenue service
   * on the given route in the given direction.
   * 
   * @param agencyId    Agency ID of stop; used only for routing requests
   *                    to federated backends
   * @param stopId      Agency-and-ID of stop being tested
   * @param routeId     Agency-and-ID of route to filter for
   * @param directionId Direction ID to filter for
   * @return true if the stop being tested ever permits boarding or alighting
   *         from the specified route in the specified direction in the 
   *         currently-loaded bundle; false otherwise
   */
  public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId,
                String routeId, String directionId);
  
  /**
   * Given a stop, test if that stop has revenue service.
   * 
   * @param agencyId Agency ID of stop; used only for routing requests
   *                 to federated backends
   * @param stopId   Agency-and-ID of stop being tested
   * @return true if the stop being tested ever permits boarding or alighting
   *         from any route in any direction in the currently-loaded bundle;
   *         false otherwise
   */
  public Boolean stopHasRevenueService(String agencyId, String stopId);

}