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

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;

import java.util.List;

/**
 * Location search result.
 * 
 * @author jmaki
 * 
 */
public class GeocodeResult implements SearchResult {

	private NycGeocoderResult result;

	private List<SearchResult> nearbyRoutes;

	public GeocodeResult(NycGeocoderResult result, List<SearchResult> nearbyRoutes) {
		this.result = result;
		this.nearbyRoutes = nearbyRoutes;
	}

	public String getFormattedAddress() {
		return result.getFormattedAddress();
	}

	public String getNeighborhood() {
		return result.getNeighborhood();
	}

	public Double getLatitude() {
		return result.getLatitude();
	}

	public Double getLongitude() {
		return result.getLongitude();
	}

	public Boolean getIsRegion() {
		return result.isRegion();
	}

	public CoordinateBounds getBounds() {
		return result.getBounds();
	}

	public List<SearchResult> getNearbyRoutes() {
		return nearbyRoutes;
	}
}
