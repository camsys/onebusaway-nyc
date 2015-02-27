package org.onebusaway.nyc.presentation.model;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.StopBean;

public class StopRouteDirection {
	
	private StopBean stopBean;
	
	private List<RouteDirection> routeDirections = new ArrayList<RouteDirection>();
	
	public StopRouteDirection(StopBean stopBean){
		this.setStop(stopBean);
	}
	
	public StopRouteDirection(StopBean stopBean, RouteDirection routeDirection){
		this.setStop(stopBean);
		routeDirections.add(routeDirection);
	}

	public void addRouteDirection(RouteDirection routeDirection) {
		routeDirections.add(routeDirection);
	}
	
	public List<RouteDirection> getRouteDirections() {
		return routeDirections;
	}

	public StopBean getStop() {
		return stopBean;
	}

	public void setStop(StopBean stopBean) {
		this.stopBean = stopBean;
	}


}
