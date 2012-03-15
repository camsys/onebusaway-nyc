package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.transit_data.model.RouteBean;

import java.util.List;

/**
 * Route available at a stop, the top being the top-level result.
 * @author jmaki
 *
 */
public class RouteAtStop {

  private RouteBean route;
    
  private List<RouteDirection> directions;

  public RouteAtStop(RouteBean route, List<RouteDirection> directions) {
    this.route = route;
    this.directions = directions;
  }
  
  public String getShortName() {
    return route.getShortName();
  }
  
  public List<RouteDirection> getDirections() {
    return directions;
  }  

}
