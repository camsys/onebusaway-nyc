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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;

/**
 * Default implementation of {@link DepotDataProviderService}
 * @author abelsare
 *
 */
public class DepotDataProviderServiceImpl implements DepotDataProviderService {

	private MostRecentFilePicker depotAssignsMostRecentFilePicker;
	private TcipMappingTool mappingTool;
	
	private static Logger log = LoggerFactory.getLogger(DepotDataProviderServiceImpl.class);

	/**
	 * Retreives
	 * @param depotIdTranslator for depot id translation
	 * @return
	 */
	@Override
	public VehicleDepotData getVehicleDepotData(DepotIdTranslator depotIdTranslator) {
		File mostRecentDepotAssignsFile = depotAssignsMostRecentFilePicker.getMostRecentSourceFile();

		log.debug("Getting VehicleDepotData object in getVehicleDepotData from " + mostRecentDepotAssignsFile.getPath());

		VehicleDepotData vehicleDepotData = null;

		MtaBusDepotFileToDataCreator busDepotFileToDataCreator;
		try {
			busDepotFileToDataCreator = new MtaBusDepotFileToDataCreator(mostRecentDepotAssignsFile);

			if (depotIdTranslator == null) {
				log.info("Depot ID translation has not been enabled properly. Depot ids will not be translated.");
			} else {
				log.info("Using depot ID translation.");
			}
			busDepotFileToDataCreator.setDepotIdTranslator(depotIdTranslator);

			vehicleDepotData = busDepotFileToDataCreator.generateVehicleDepotData();
		} catch (IOException e) {
			log.info("Could not create data object from " + mostRecentDepotAssignsFile.getPath());
			log.info(e.getMessage());
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		log.debug("Returning VehicleDepotData object in getVehicleDepotData.");
		return vehicleDepotData;
	}

	@Override
	public List<Vehicle> buildResponseData(List<CPTFleetSubsetGroup> depotGroups) {
		List<Vehicle> vehicles = new ArrayList<Vehicle>();
		
		//Loop through all depot groups and create vehicles with the required info
		for(CPTFleetSubsetGroup depotGroup : depotGroups) {
			List<CPTVehicleIden> tcipVehciles = depotGroup.getGroupMembers().getGroupMember();
			for(CPTVehicleIden tcipVehicle : tcipVehciles) {
				Vehicle vehicle = new Vehicle();
				
				vehicle.setDepotId(depotGroup.getGroupGarage().getFacilityName());
				vehicle.setVehicleId(String.valueOf(tcipVehicle.getVehicleId()));
				vehicle.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(tcipVehicle.getAgencyId()));
				
				vehicles.add(vehicle);
			}
		}
		
		return vehicles;
	}

	/**
	 * Injects most recent file picker for Depot Assigns (SPEAR) Data
	 * @param mostRecentFilePicker the mostRecentFilePicker to set
	 */
	@Autowired
	@Qualifier("depotFilePicker")
	public void setMostRecentFilePicker(MostRecentFilePicker mostRecentFilePicker) {
		this.depotAssignsMostRecentFilePicker = mostRecentFilePicker;
	}
	
	/**
	 * Injects {@link TcipMappingTool}
	 * @param mappingTool the mappingTool to set
	 */
	@Autowired
	@Qualifier("tcipMappingTool")
	public void setMappingTool(TcipMappingTool mappingTool) {
		this.mappingTool = mappingTool;
	}


}
