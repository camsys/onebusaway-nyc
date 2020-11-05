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

package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;

import tcip_final_3_0_5_1.CPTVehicleIden;

/**
 * Converts a tcip vehicle object (CPTVehicleIden) to a json vehicle object
 * (Vehicle).
 * 
 * @author sclark
 * 
 */
public class VehicleFromTcip implements
    ModelCounterpartConverter<CPTVehicleIden, Vehicle> {

	private TcipMappingTool mappingTool;

	/** {@inheritDoc} */
	public Vehicle convert(CPTVehicleIden input) {
		Vehicle outputVehicle = new Vehicle();

		outputVehicle.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(input.getAgencyId()));
		outputVehicle.setVehicleId(String.valueOf(input.getVehicleId()));

		return outputVehicle;
	}

	/**
	 * Injects {@link TcipMappingTool}
	 * @param mappingTool the mappingTool to set
	 */
	public void setMappingTool(TcipMappingTool mappingTool) {
		this.mappingTool = mappingTool;
	}


}
