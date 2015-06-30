/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data.services;

import java.util.List;

import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.services.TransitDataService;

/**
 * Extensions to the OBA TransitDataService that power NYC-specific UI features.
 * 
 * This has been merged into the TransitDataService.  Keeping for ease of integration. 
 * 
 * @author jmaki
 *
 */
public interface NycTransitDataService extends TransitDataService {
	public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String VehicleId,
			String TripId);
}
