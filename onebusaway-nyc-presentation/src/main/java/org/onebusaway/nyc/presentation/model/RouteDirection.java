package org.onebusaway.nyc.presentation.model;

import java.util.List;

import org.onebusaway.transit_data.model.StopBean;

public class RouteDirection {

	private String directionId;

	private String routeId;

	private Boolean hasUpcomingScheduledService;
	
	private List<StopRouteDirection> stopRouteDirections;

	public RouteDirection(String routeId, String directionId,
			Boolean hasUpcomingScheduledService) {
		this.routeId = routeId;
		this.directionId = directionId;
		this.hasUpcomingScheduledService = hasUpcomingScheduledService;
	}
	
	public RouteDirection(String routeId, String directionId){
		this.routeId = routeId;
		this.directionId = directionId;
	}

	public String getDirectionId() {
		return directionId;
	}

	public void setDirectionId(String directionId) {
		this.directionId = directionId;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeBean) {
		this.routeId = routeBean;
	}

	public Boolean getHasUpcomingScheduledService() {
		return hasUpcomingScheduledService;
	}

	public void setHasUpcomingScheduledService(
			Boolean hasUpcomingScheduledService) {
		this.hasUpcomingScheduledService = hasUpcomingScheduledService;
	}

	public List<StopRouteDirection> getStopRouteDirections() {
		return stopRouteDirections;
	}

	public void setStopRouteDirections(List<StopRouteDirection> stopRouteDirections) {
		this.stopRouteDirections = stopRouteDirections;
	}

}
