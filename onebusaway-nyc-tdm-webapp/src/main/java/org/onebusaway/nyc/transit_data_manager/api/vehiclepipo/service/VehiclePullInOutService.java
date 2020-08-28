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

package org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;

/**
 * Provides vehicle pull in pull out information for vehicle pipo resources
 * @author abelsare
 *
 */
public interface VehiclePullInOutService {

	/**
	 * Returns the list of active pull outs from the parsed pull out data
	 * @param all the parsed pull out data 
	 * @param include all pullout records for a given bus in case there are multiple pullouts
	 * @return list of active pull outs
	 */
	List<VehiclePullInOutInfo> getActivePullOuts(List<VehiclePullInOutInfo> allPullouts, 
			boolean includeAllPullouts);
	
	/**
	 * Returns the most recent active pull out from a collection of active pull outs.
	 * @param activePullouts collection of active pull outs
	 * @return most recent active pull out 
	 */
	VehiclePullInOutInfo getMostRecentActivePullout(List<VehiclePullInOutInfo> activePullouts);
}
