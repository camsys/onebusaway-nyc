package org.onebusaway.nyc.webapp.actions.api.siri.model;

import java.util.List;

import org.onebusaway.transit_data.model.StopBean;

public class RouteForDirection {

	private String directionId;
	
	private String destination;

	private String routeId;

	private Boolean hasUpcomingScheduledService;
	
	private List<StopOnRoute> stops;

	public RouteForDirection(String routeId, String directionId,
			Boolean hasUpcomingScheduledService) {
		this.routeId = routeId;
		this.directionId = directionId;
		this.hasUpcomingScheduledService = hasUpcomingScheduledService;
		this.setDestination(null);
	}
	
	public RouteForDirection(String routeId, String directionId){
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

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public List<StopOnRoute> getStops() {
		return stops;
	}

	public void setStops(List<StopOnRoute> stops) {
		this.stops = stops;
	}

}
