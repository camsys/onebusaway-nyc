package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.nyc.webapp.model.RouteDetails;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests for a particular route's geometry
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class RouteAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private TransitDataService transitService;
  
  private List<String> routeIds;
  private List<RouteDetails> routesDetails = new ArrayList<RouteDetails>();
  
  @Override
  public String execute() throws Exception {
    for (String routeId : routeIds) {
      RouteDetails routeDetails = makeRouteDetails(routeId);
      routesDetails.add(routeDetails);
    }

    return SUCCESS;
  }

  private RouteDetails makeRouteDetails(String routeId) {
    RouteBean routeBean = transitService.getRouteForId(routeId);
    String shortName = routeBean.getShortName();
    String description = routeBean.getLongName();
    
    StopsForRouteBean stopsForRoute = transitService.getStopsForRoute(routeId);
    List<EncodedPolylineBean> polylines = stopsForRoute.getPolylines();
    if (polylines.isEmpty()) {
      throw new IllegalStateException("no polylines for route: " + routeId);
    }
    
    // FIXME last update time
    String lastUpdated = "1 minute ago";
    RouteDetails routeDetails = new RouteDetails(routeId, shortName, description, lastUpdated, polylines);
    return routeDetails;
  }

  public void setRouteId(String[] routeIds) {
    this.routeIds = Arrays.asList(routeIds);
  }
  
  public List<RouteDetails> getRoutes() {
    return routesDetails;
  }
}
