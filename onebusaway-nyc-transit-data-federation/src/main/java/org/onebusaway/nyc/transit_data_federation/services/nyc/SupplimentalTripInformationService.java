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

package org.onebusaway.nyc.transit_data_federation.services.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.SupplimentalTripInformation;

/**
 * Get additional non-customer information for a trip. 
 * Includes Direction, Trip Type, and Bus Type 
 * @author laidig
 *
 */
public interface SupplimentalTripInformationService {
	public SupplimentalTripInformation getSupplimentalTripInformation(String tripId);
	public SupplimentalTripInformation getSupplimentalTripInformation(AgencyAndId tripId);
}
