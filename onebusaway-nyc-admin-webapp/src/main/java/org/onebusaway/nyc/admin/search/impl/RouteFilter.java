package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given route
 * @author abelsare
 *
 */
public class RouteFilter implements Filter<VehicleStatus>{

	private String route;
	
	public RouteFilter(String route) {
		this.route = route;
	}
	
	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getRoute())) {
			return type.getRoute().equalsIgnoreCase(route);
		}
		return false;
	}

}
