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

import org.onebusaway.federations.annotations.FederatedByAgencyIdMethod;
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
	
	  /**
	   * Given a stop, route, and direction, test if that stop has revenue service
	   * on the given route in the given direction.
	   * 
	   * @param agencyId    Agency ID of stop; used only for routing requests
	   *                    to federated backends
	   * @param stopId      Agency-and-ID of stop being tested
	   * @param routeId     Agency-and-ID of route to filter for
	   * @param directionId Direction ID to filter for
	   * @return true if the stop being tested ever permits boarding or alighting
	   *         from the specified route in the specified direction in the 
	   *         currently-loaded bundle; false otherwise
	   */
	  public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId,
	                String routeId, String directionId);
	  
	  /**
	   * Given a stop, test if that stop has revenue service.
	   * 
	   * @param agencyId Agency ID of stop; used only for routing requests
	   *                 to federated backends
	   * @param stopId   Agency-and-ID of stop being tested
	   * @return true if the stop being tested ever permits boarding or alighting
	   *         from any route in any direction in the currently-loaded bundle;
	   *         false otherwise
	   */
	  public Boolean stopHasRevenueService(String agencyId, String stopId);
}
