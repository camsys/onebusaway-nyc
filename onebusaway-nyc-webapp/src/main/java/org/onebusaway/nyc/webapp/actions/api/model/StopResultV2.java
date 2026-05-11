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

import org.apache.commons.codec.digest.DigestUtils;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.StopBean;

import java.util.Comparator;
import java.util.List;

/**
 * A stop as a top-level search result.
 * 
 * @author jmaki
 * 
 */
public class StopResultV2 implements SearchResult {

	private StopBean stop;

	private List<RouteAtStopV2> routesAvailable;

	public StopResultV2(StopBean stop, List<RouteAtStopV2> routesAvailable) {
		this.stop = stop;
		this.routesAvailable = routesAvailable;
	}

	public String getId() {
		return stop.getId();
	}

	public String getName() {
		return stop.getName();
	}

	public Double getLatitude() {
		return stop.getLat();
	}

	public Double getLongitude() {
		return stop.getLon();
	}

	public String getStopDirection() {
		if (stop.getDirection() == null || (stop.getDirection() != null && stop.getDirection().equals("?"))) {
			return "unknown";
		} else {
			return stop.getDirection();
		}
	}

	public List<RouteAtStopV2> getRoutesAvailable() {
		return routesAvailable;
	}

	@Override
	public String getEtag() {
		StringBuilder sb = new StringBuilder();
		// Sort the directions by directionId to ensure deterministic output
		routesAvailable.stream()
				.sorted(Comparator.comparing(RouteAtStopV2::getId))
				.forEach(routeAtStopV2 -> {
					sb.append(routeAtStopV2.getId());
					routeAtStopV2.getDirections().stream()
							.sorted(Comparator.comparing(RouteDirectionV2::getDirectionId))
							.forEach(direction -> {
								sb.append(direction.getDirPolyAndStopsHash());
								sb.append("||");
							});
				});
		return DigestUtils.md5Hex(sb.toString());
	}
}
