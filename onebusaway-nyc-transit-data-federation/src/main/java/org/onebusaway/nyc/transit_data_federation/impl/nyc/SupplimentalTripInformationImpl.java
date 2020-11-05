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

package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.model.nyc.SupplimentalTripInformation;
import org.onebusaway.nyc.transit_data_federation.services.nyc.SupplimentalTripInformationService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.springframework.beans.factory.annotation.Autowired;

public class SupplimentalTripInformationImpl implements SupplimentalTripInformationService {
	
	private Map<AgencyAndId, SupplimentalTripInformation> _tripInfo;
	
	@Autowired
	private NycFederatedTransitDataBundle _bundle;
		
	@PostConstruct
	@Refreshable(dependsOn = NycRefreshableResources.SUPPLIMENTAL_TRIP_DATA)
	public void setup() throws IOException, ClassNotFoundException {
		File tripInfoPath = _bundle.getSupplimentalTrioInfo();
		
		if (tripInfoPath.exists()) {
			_tripInfo = ObjectSerializationLibrary.readObject(tripInfoPath);
		} else {
			_tripInfo = null;
		}
		
	}

	@Override
	public SupplimentalTripInformation getSupplimentalTripInformation(String trip) {
			String[] tripParts = trip.split("_");
			if (tripParts.length != 2) {
				throw new IllegalArgumentException(trip + " does not look like a fully qualified trip_id");
			}
			AgencyAndId tripId = new AgencyAndId(tripParts[0], tripParts[1]);
			return getSupplimentalTripInformation(tripId);
	}

	@Override
	public SupplimentalTripInformation getSupplimentalTripInformation(AgencyAndId tripId) {
		return _tripInfo.get(tripId);
	}

}
