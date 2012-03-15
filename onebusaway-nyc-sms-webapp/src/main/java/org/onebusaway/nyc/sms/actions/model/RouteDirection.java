package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

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

  private List<String> distanceAways;
  
  public RouteDirection(StopGroupBean stopGroup, Boolean hasUpcomingScheduledService, List<String> distanceAways, List<NaturalLanguageStringBean> serviceAlerts) {
    this.destination = stopGroup.getName().getName();    
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAways = distanceAways;
    this.serviceAlerts = serviceAlerts;
  }

  public String getDestination() {
    return destination;
  }

  public Boolean hasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }
    
  public List<String> getDistanceAways() {
    return distanceAways;
  }
  
  public List<NaturalLanguageStringBean> getSerivceAlerts() {
    return serviceAlerts;
  }

}
