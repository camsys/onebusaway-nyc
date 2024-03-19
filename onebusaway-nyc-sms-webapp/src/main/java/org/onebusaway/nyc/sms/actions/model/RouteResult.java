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
import org.onebusaway.transit_data.model.RouteBean;

import java.io.Serializable;
import java.util.List;

/**
 * Route as a top-level search result.
 * 
 * @author jmaki
 * 
 */
public class RouteResult implements SearchResult, Serializable {

	private static final long serialVersionUID = 1L;

	private int type;

	private RouteBean route;

	private List<RouteDirection> directions;

	public RouteResult(RouteBean route, List<RouteDirection> directions) {
		this.route = route;
		this.directions = directions;
	}

	public RouteResult(RouteBean route, List<RouteDirection> directions, int type) {
		this.route = route;
		this.directions = directions;
		this.type = type;
	}

	public String getShortName() {
		return route.getShortName();
	}

	public List<RouteDirection> getDirections() {
		return directions;
	}
}
