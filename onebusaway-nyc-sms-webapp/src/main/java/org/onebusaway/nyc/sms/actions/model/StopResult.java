package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.io.Serializable;
import java.util.List;

/**
 * A stop as a top-level search result.
 * 
 * @author jmaki
 * 
 */
public class StopResult implements SearchResult, Serializable {

	private static final long serialVersionUID = 1L;

	private StopBean stop;

	private List<RouteAtStop> routesAvailable;
	
	private boolean matchesRouteIdFilter;
	
	private Double distanceToQueryLocation = null;

	public StopResult(StopBean stop, List<RouteAtStop> routesAvailable, boolean matchesRouteIdFilter) {
		this.stop = stop;
		this.routesAvailable = routesAvailable;
		this.matchesRouteIdFilter = matchesRouteIdFilter;
	}

	public String getIdWithoutAgency() {
		return AgencyAndIdLibrary.convertFromString(stop.getId()).getId();
	}

	public String getStopDirection() {
		return stop.getDirection();
	}

	public List<RouteAtStop> getRoutesAvailable() {
		return routesAvailable;
	}
	
	public boolean matchesRouteIdFilter() {
	  return matchesRouteIdFilter;
	}
	
	public StopBean getStop() {
	  return stop;
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
