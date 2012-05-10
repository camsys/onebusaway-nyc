package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;

/**
 * Route available near or within an area.
 * 
 * @author jmaki
 * 
 */
public class RouteInRegionResult implements SearchResult {

	private RouteBean route;

	public RouteInRegionResult(RouteBean route) {
		this.route = route;
	}

	public String getShortName() {
		return route.getShortName();
	}

	@Override
	public void setDistanceToQueryLocation(Double distance) {
		// TODO Auto-generated method stub

	}

	@Override
	public Double getDistanceToQueryLocation() {
		// TODO Auto-generated method stub
		return null;
	}

}
