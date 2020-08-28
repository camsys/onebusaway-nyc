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

package org.onebusaway.nyc.transit_data_manager.api.service;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.DepotResource;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;


/**
 * Reads and parses depot data from the input files and provides data to {@link DepotResource}. 
 * It checks the most recent input file received and generates objects required by the resource.
 * @author abelsare
 *
 */
public interface DepotDataProviderService {
	
	/**
	 * Processes most recent input file and builds {@link VehicleDepotData}
	 * object with all depot records.
	 * @param depotIdTranslator for depot id translation
	 * @return depot data in the form required by API resources
	 */
	VehicleDepotData getVehicleDepotData(DepotIdTranslator depotIdTranslator);
	
	/**
	 * Builds vehicle depot assignment data from a list of depot groups
	 * @param depotGroups list of depot groups
	 * @return vehicle depot assignments.
	 */
	List<Vehicle> buildResponseData(List<CPTFleetSubsetGroup> depotGroups);

}
