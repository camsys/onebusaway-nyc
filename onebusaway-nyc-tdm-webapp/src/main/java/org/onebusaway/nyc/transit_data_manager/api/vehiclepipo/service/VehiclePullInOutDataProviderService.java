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

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.VehiclePipoResource;

/**
 * Reads and parses data from the input files and provides data to {@link VehiclePipoResource}. 
 * It checks the most recent input file received and generates objects required by the resources.
 * @author abelsare
 *
 */
public interface VehiclePullInOutDataProviderService {
	
	/**
	 * Processes most recent input file and builds {@link ImporterVehiclePulloutData}
	 * object with all pullout records.
	 * @param depotIdTranslator for depot id translation
	 * @return pullout data in the form required by API resources
	 */
	ImporterVehiclePulloutData getVehiclePipoData(DepotIdTranslator depotIdTranslator);
	
	/**
	 * Builds vehicle pullinout data as required to generate response to the api call
	 * @param vehiclePullInOuts parsed and generated vehicle pullout data
	 * @return vehicle pullouts with information required by the response.
	 */
	List<PullInOut> buildResponseData(List<VehiclePullInOutInfo> vehiclePullInOuts);

}
