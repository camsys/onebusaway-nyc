package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;

import java.util.List;

/**
 * Route as a top-level search result.
 * 
 * @author jmaki
 * 
 */
public class RouteResult implements SearchResult {

	private RouteBean route;

	private List<RouteDirection> directions;
	
	private Double distanceToQueryLocation = null;

	public RouteResult(RouteBean route, List<RouteDirection> directions) {
		this.route = route;
		this.directions = directions;
	}

	public String getId() {
		return route.getId();
	}

	public String getShortName() {
		return route.getShortName();
	}

	public String getLongName() {
		return route.getLongName();
	}

	public String getDescription() {
		return route.getDescription();
	}

	public String getColor() {
		if (route.getColor() != null) {
			return route.getColor();
		} else {
			return "000000";
		}
	}

	public List<RouteDirection> getDirections() {
		return directions;
	}

	@Override
	public void setDistanceToQueryLocation(Double distance) {
		this.distanceToQueryLocation = distance;
	}

	@Override
	public Double getDistanceToQueryLocation() {
		return this.distanceToQueryLocation;
	}

}
