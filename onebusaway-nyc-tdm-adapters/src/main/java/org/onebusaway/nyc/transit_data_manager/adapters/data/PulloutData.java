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

package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;


public interface PulloutData {
	
	/**
	 * Returns all pull out data for all buses
	 * @return pull out data
	 */
	List<VehiclePullInOutInfo> getAllPullouts();
	
	/**
	 * Returns pull out data for the given bus number
	 * @param busNumber bus number for which pull out data is desired
	 * @return pull out data for the given bus number
	 */
	List<VehiclePullInOutInfo> getPulloutsByBus(Long busNumber);
	
	/**
	 * Returns pull out data for the given depot
	 * @param depotId depot for which pull data is desired
	 * @return pull out data for given depot
	 */
	List<VehiclePullInOutInfo> getPulloutsByDepot(String depotId);
	
	/**
	 * Returns pull out data for the given agency
	 * @param agencyId agency for which pull data is desired
	 * @return pull out data for given agency
	 */
	List<VehiclePullInOutInfo> getPulloutsByAgency(String agencyId);
}
