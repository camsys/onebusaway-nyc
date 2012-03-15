package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.util.List;

/**
 * A stop as a top-level search result.
 * @author jmaki
 *
 */
public class StopResult implements SearchResult {

  private StopBean stop;
  
  private List<RouteAtStop> routesAvailable;
  
  public StopResult(StopBean stop, List<RouteAtStop> routesAvailable) {
    this.stop = stop;
    this.routesAvailable = routesAvailable;
  }

  public String getIdWithoutAgency() {
    return AgencyAndIdLibrary.convertFromString(stop.getId()).getId();
  }

  public String getStopDirection() {
   return stop.getDirection();
  }  
  
  public List<RouteAtStop> getRoutesAvailable() {
    return routesAvailable;
  }
  
}
