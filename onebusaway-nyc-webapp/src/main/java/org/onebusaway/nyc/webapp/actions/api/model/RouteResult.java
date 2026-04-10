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

package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Route as a top-level search result.
 *
 * @author jmaki
 *
 */
public class RouteResult implements SearchResult {

	private RouteBean route;

	private List<RouteDirection> directions;

	private Collection<TripModificationDiffView> tripModificationDiffs;

	public RouteResult(RouteBean route, List<RouteDirection> directions) {
		this.route = route;
		this.directions = directions;
	}

	public RouteResult(RouteBean route, List<RouteDirection> directions, Collection<TripModificationDiff> tripModificationDiffs) {
		this.route = route;
		this.directions = directions;
		String routeId = route.getId();
		this.tripModificationDiffs = tripModificationDiffs == null ? null :
				tripModificationDiffs.stream()
						.map(diff -> new TripModificationDiffView(diff, routeId))
						.collect(Collectors.toList());
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

	public int getType() {return route.getType();}

	public List<RouteDirection> getDirections() {
		return directions;
	}

	public Collection<TripModificationDiffView> getTripModificationDiffs() {
		return tripModificationDiffs;
	}
}
