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

import tcip_final_4_0_0_0.CPTFleetSubsetGroup;
import tcip_final_4_0_0_0.CPTVehicleIden;

/**
 * Default implementation of {@link DepotDataProviderService}
 * @author abelsare
 *
 */
public class DepotDataProviderServiceImpl implements DepotDataProviderService {

	private MostRecentFilePicker mostRecentFilePicker;
	private TcipMappingTool mappingTool;
	
	private static Logger log = LoggerFactory.getLogger(DepotDataProviderServiceImpl.class);
	
	@Override
	public VehicleDepotData getVehicleDepotData(DepotIdTranslator depotIdTranslator) {
		File inputFile = mostRecentFilePicker.getMostRecentSourceFile();

		log.debug("Getting VehicleDepotData object in getVehicleDepotData from " + inputFile.getPath());

		VehicleDepotData resultData = null;

		MtaBusDepotFileToDataCreator process;
		try {
			process = new MtaBusDepotFileToDataCreator(inputFile);

			if (depotIdTranslator == null) {
				log.info("Depot ID translation has not been enabled properly. Depot ids will not be translated.");
			} else {
				log.info("Using depot ID translation.");
			}
			process.setDepotIdTranslator(depotIdTranslator);

			resultData = process.generateDataObject();
		} catch (IOException e) {
			log.info("Could not create data object from " + inputFile.getPath());
			log.info(e.getMessage());
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		log.debug("Returning VehicleDepotData object in getVehicleDepotData.");
		return resultData;
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
	 * Injects most recent file picker
	 * @param mostRecentFilePicker the mostRecentFilePicker to set
	 */
	@Autowired
	@Qualifier("depotFilePicker")
	public void setMostRecentFilePicker(MostRecentFilePicker mostRecentFilePicker) {
		this.mostRecentFilePicker = mostRecentFilePicker;
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
