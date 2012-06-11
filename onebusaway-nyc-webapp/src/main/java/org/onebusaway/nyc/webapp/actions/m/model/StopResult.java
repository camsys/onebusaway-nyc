package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * A stop as a top-level search result.
 * 
 * @author jmaki
 * 
 */
public class StopResult implements SearchResult {

	private StopBean stop;

	private List<RouteAtStop> routesWithArrivals;

	private List<RouteAtStop> routesWithNoVehiclesEnRoute;

	private List<RouteAtStop> routesWithNoScheduledService;
	
	private List<RouteBean> filteredRoutes;
	
	private Double distanceToQueryLocation = null;

	public StopResult(StopBean stop, List<RouteAtStop> routesWithArrivals, List<RouteAtStop> routesWithNoVehiclesEnRoute,
			List<RouteAtStop> routesWithNoScheduledService, List<RouteBean> filteredRoutes) {
		this.stop = stop;
		this.routesWithArrivals = routesWithArrivals;
		this.routesWithNoVehiclesEnRoute = routesWithNoVehiclesEnRoute;
		this.routesWithNoScheduledService = routesWithNoScheduledService;
		this.filteredRoutes = filteredRoutes;
	}

	public String getId() {
		return stop.getId();
	}

	public String getIdWithoutAgency() {
		return AgencyAndIdLibrary.convertFromString(getId()).getId();
	}

	public String getName() {
		return stop.getName();
	}

	public List<RouteAtStop> getAllRoutesAvailable() {
		List<RouteAtStop> fullList = new ArrayList<RouteAtStop>();
		fullList.addAll(routesWithArrivals);
		fullList.addAll(routesWithNoVehiclesEnRoute);
		fullList.addAll(routesWithNoVehiclesEnRoute);

		return fullList;
	}

	public List<RouteAtStop> getRoutesWithNoVehiclesEnRoute() {
		return routesWithNoVehiclesEnRoute;
	}

	public List<RouteAtStop> getRoutesWithNoScheduledService() {
		return routesWithNoScheduledService;
	}

	public List<RouteAtStop> getRoutesWithArrivals() {
		return routesWithArrivals;
	}
	
	public List<RouteBean> getFilteredRoutes() {
	  return filteredRoutes;
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
