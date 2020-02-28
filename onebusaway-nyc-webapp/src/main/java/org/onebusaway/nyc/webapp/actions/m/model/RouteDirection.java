package org.onebusaway.nyc.webapp.actions.m.model;

import org.apache.xpath.operations.Bool;
import org.onebusaway.transit_data.model.StopGroupBean;

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
  
  private List<String> distanceAways;

  private List<Boolean> hasRealtimes;
  
  private Boolean hasUpcomingScheduledService;

  public RouteDirection(String destinationName, StopGroupBean stopGroup, List<StopOnRoute> stops, Boolean hasUpcomingScheduledService, List<String> distanceAways) {
    this.directionId = stopGroup.getId();
    this.destination = destinationName;    
    this.stops = stops;
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAways = distanceAways;
  }

  public RouteDirection(String destinationName, StopGroupBean stopGroup, List<StopOnRoute> stops, Boolean hasUpcomingScheduledService, List<String> distanceAways, List<Boolean> hasRealtimes) {
    this.directionId = stopGroup.getId();
    this.destination = destinationName;
    this.stops = stops;
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAways = distanceAways;
    this.hasRealtimes = hasRealtimes;
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
  
  public List<String> getDistanceAways() {
    return distanceAways;
  }

  public List<Boolean> getHasRealtimes() { return hasRealtimes; }

  public void setHasRealtimes(List<Boolean> hasRealtimes) { this.hasRealtimes=hasRealtimes; }

}
