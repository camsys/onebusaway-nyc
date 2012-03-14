package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.List;

/**
 * Route destination
 * @author jmaki
 *
 */
public class RouteDirection {

  private String directionId;
  
  private String destination;
  
  private List<StopOnRoute> stops;
  
  private List<NaturalLanguageStringBean> serviceAlerts; 

  private List<String> distanceAways;
  
  private Boolean hasUpcomingScheduledService;

  public RouteDirection(StopGroupBean stopGroup, List<StopOnRoute> stops, Boolean hasUpcomingScheduledService, 
      List<NaturalLanguageStringBean> serviceAlerts, List<String> distanceAways) {
    this.directionId = stopGroup.getId();
    this.destination = stopGroup.getName().getName();    
    this.stops = stops;
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.serviceAlerts = serviceAlerts;
    this.distanceAways = distanceAways;
  }

  public String getDirectionId() {
    return directionId;
  }
  
  public String getDestination() {
    return destination;
  }

  public List<StopOnRoute> getStops() {
    return stops;
  }
  
  public Boolean getHasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }
  
  public List<NaturalLanguageStringBean> getServiceAlerts() {
    return serviceAlerts;
  }

  public List<String> getDistanceAways() {
    return distanceAways;
  }

}
