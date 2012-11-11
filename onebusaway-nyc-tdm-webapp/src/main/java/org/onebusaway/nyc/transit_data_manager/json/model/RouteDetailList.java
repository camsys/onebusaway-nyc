package org.onebusaway.nyc.transit_data_manager.json.model;

import java.util.ArrayList;

public class RouteDetailList extends JsonMessage {
  private ArrayList<RouteDetail> routes = new ArrayList<RouteDetail>();

  public ArrayList<RouteDetail> getRoutes() {
    return routes;
  }

  public void setRoutes(ArrayList<RouteDetail> routes) {
    this.routes = routes;
  }

}
