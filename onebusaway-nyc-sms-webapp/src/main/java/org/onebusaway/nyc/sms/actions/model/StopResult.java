/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
}
