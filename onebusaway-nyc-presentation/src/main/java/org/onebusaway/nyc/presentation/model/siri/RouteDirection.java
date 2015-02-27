package org.onebusaway.nyc.presentation.model.siri;

import org.onebusaway.transit_data.model.RouteBean;

public class RouteDirection {

	private String directionId;

	private RouteBean routeBean;

	private Boolean hasUpcomingScheduledService;

	public RouteDirection(RouteBean routeBean, String directionId,
			Boolean hasUpcomingScheduledService) {
		this.routeBean = routeBean;
		this.directionId = directionId;
		this.hasUpcomingScheduledService = hasUpcomingScheduledService;
	}

	public String getDirectionId() {
		return directionId;
	}

	public void setDirectionId(String directionId) {
		this.directionId = directionId;
	}

	public RouteBean getRouteBean() {
		return routeBean;
	}

	public void setRouteBean(RouteBean routeBean) {
		this.routeBean = routeBean;
	}

	public Boolean getHasUpcomingScheduledService() {
		return hasUpcomingScheduledService;
	}

	public void setHasUpcomingScheduledService(
			Boolean hasUpcomingScheduledService) {
		this.hasUpcomingScheduledService = hasUpcomingScheduledService;
	}

}
