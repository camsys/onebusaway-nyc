package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.DistanceAway;
import org.onebusaway.nyc.webapp.model.StopDetails;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests for detailed info for a particular stop, suitable for a popup
 */
@ParentPackage("json-default")
@Result(type="json")
public class StopAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  private StopDetails stopDetails;
  private String stopId;
  
  @Autowired
  private TransitDataService service;
  
  @Override
  public String execute() throws Exception {
    stopDetails = makeStopDetails(stopId);
    return SUCCESS;
  }
  
  private StopDetails makeStopDetails(String stopId) {
    StopBean stop = service.getStop(stopId);
    
    String stopName = stop.getName();
    List<Double> latLng = makeLatLng(stop.getLat(), stop.getLon());
    // FIXME set last update time appropriately
    String lastUpdateTime = "one minute ago";
    
    List<AvailableRoute> availableRoutes = new ArrayList<AvailableRoute>();
    List<RouteBean> routes = stop.getRoutes();        
    for (RouteBean routeBean : routes) {
      String shortName = routeBean.getShortName();
      String longName = routeBean.getLongName();
      
      // FIXME stubbed out distance aways as we don't have it in the api yet
      List<DistanceAway> distanceAways = Arrays.asList(
          new DistanceAway[] {new DistanceAway(0, 0)});
      availableRoutes.add(new AvailableRoute(shortName, longName, distanceAways));
    }
    
    return new StopDetails(stopId, stopName, latLng, lastUpdateTime, availableRoutes);
  }

  public void setStopId(String stopId) {
    this.stopId = stopId;
  }
  
  public StopDetails getStop() {
    return stopDetails;
  }

}
