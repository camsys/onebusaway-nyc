package org.onebusaway.nyc.webapp.actions.api.siri.model;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.StopBean;

public class StopRouteDirection {
	
	private StopBean stopBean;
	
	private List<RouteForDirection> routeDirections = new ArrayList<RouteForDirection>();
	
	public StopRouteDirection(StopBean stopBean){
		this.setStop(stopBean);
	}
	
	public StopRouteDirection(StopBean stopBean, RouteForDirection routeDirection){
		this.setStop(stopBean);
		routeDirections.add(routeDirection);
	}

	public void addRouteDirection(RouteForDirection routeDirection) {
		routeDirections.add(routeDirection);
	}
	
	public List<RouteForDirection> getRouteDirections() {
		return routeDirections;
	}

	public StopBean getStop() {
		return stopBean;
	}

	public void setStop(StopBean stopBean) {
		this.stopBean = stopBean;
	}


}
