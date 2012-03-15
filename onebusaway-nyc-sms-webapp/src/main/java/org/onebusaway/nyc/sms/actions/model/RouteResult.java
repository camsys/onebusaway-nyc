package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;

import java.util.List;

/**
 * Route as a top-level search result.
 * @author jmaki
 *
 */
public class RouteResult implements SearchResult {

  private RouteBean route;
  
  private List<RouteDirection> directions;
      
  public RouteResult(RouteBean route, List<RouteDirection> directions) {
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
