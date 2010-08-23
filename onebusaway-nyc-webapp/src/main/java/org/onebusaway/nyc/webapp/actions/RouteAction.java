package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.RouteDetails;

/**
 * Handles requests for a particular route's geometry
 */
@ParentPackage("json-default")
@Result(type="json")
public class RouteAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  private String routeId;
  private RouteDetails routeDetails;
  
  @Override
  public String execute() throws Exception {
    routeDetails = makeRouteDetails(routeId);
    return SUCCESS;
  }

  // FIXME stubbed data
  private RouteDetails makeRouteDetails(String routeId) {
    List<List<Double>> routeLatLngs = new ArrayList<List<Double>>();
    String description;

    if (routeId.equals("M14D")) {
      description = "14th Street Crosstown via Avenue D";
      
      routeLatLngs.add(makeLatLng(40.71841194933421, -74.0005588413086));
      routeLatLngs.add(makeLatLng(40.71785899481434, -73.99978636511231));
      routeLatLngs.add(makeLatLng(40.717078345319955, -73.9985418201294));
      routeLatLngs.add(makeLatLng(40.716200103696565, -73.99605273016358));
      routeLatLngs.add(makeLatLng(40.713923127013494, -73.99746893652345));
      routeLatLngs.add(makeLatLng(40.71353278033439, -73.99867056616212));
    } else {
      description = "14th Street Crosstown via Avenue A";
      
      routeLatLngs.add(makeLatLng(40.70998702652973, -73.99193285711671));
      routeLatLngs.add(makeLatLng(40.71460622819519, -73.99283407934571));
      routeLatLngs.add(makeLatLng(40.71912753071832, -73.99034498937989));
    }

    return new RouteDetails(routeId, routeId, description, "1 minute ago", routeLatLngs);
  }
  
  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }
  
  public RouteDetails getRoute() {
    return routeDetails;
  }

}
