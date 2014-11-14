package org.onebusaway.nyc.transit_data_manager.api.service;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.DepotResource;

import tcip_final_4_0_0_0.CPTFleetSubsetGroup;


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
