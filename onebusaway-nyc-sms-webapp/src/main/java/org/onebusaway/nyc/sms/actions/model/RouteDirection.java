package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Route destination--without stops.
 * @author jmaki
 *
 */
public class RouteDirection {

  private String destination;
    
  private Boolean hasUpcomingScheduledService;

  private List<NaturalLanguageStringBean> serviceAlerts;

  private HashMap<Double, String> distanceAwaysByDistanceFromStrings;
  
  public RouteDirection(StopGroupBean stopGroup, Boolean hasUpcomingScheduledService, HashMap<Double, String> distanceAwaysByDistanceFromString, List<NaturalLanguageStringBean> serviceAlerts) {
    this.destination = stopGroup.getName().getName();    
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAwaysByDistanceFromStrings = distanceAwaysByDistanceFromString;
    this.serviceAlerts = serviceAlerts;
  }

  public String getDestination() {
    return destination;
  }

  public Boolean hasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }
    
  public List<String> getDistanceAways() {
    return new ArrayList<String>(distanceAwaysByDistanceFromStrings.values());
  }

  public HashMap<Double, String> getDistanceAwaysWithSortKey() {
    return distanceAwaysByDistanceFromStrings;
  }

  public List<NaturalLanguageStringBean> getSerivceAlerts() {
    return serviceAlerts;
  }

}
