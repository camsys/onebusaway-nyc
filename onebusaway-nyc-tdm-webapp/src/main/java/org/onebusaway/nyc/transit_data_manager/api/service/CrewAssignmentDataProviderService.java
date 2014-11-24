package org.onebusaway.nyc.transit_data_manager.api.service;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.CrewResource;

import tcip_final_4_0_0.SCHOperatorAssignment;

/**
 * Reads and parses data from the input files and provides data to {@link CrewResource}. 
 * It checks the most recent input file received and generates objects required by the resources.
 * @author abelsare
 *
 */
public interface CrewAssignmentDataProviderService {

	/**
	 * Processes most recent input file and builds {@link OperatorAssignmentData}
	 * object with all crew assignment records.
	 * @param depotIdTranslator for depot id translation
	 * @return crew assignment data in the form required by API resources
	 */
	OperatorAssignmentData getCrewAssignmentData(DepotIdTranslator depotIdTranslator);
	
	/**
	 * Builds crew assignment data as required to generate response to the api call
	 * @param crewAssignments parsed and generated crew assignments
	 * @return crew assignments with information required by the response.
	 */
	List<OperatorAssignment> buildResponseData(List<SCHOperatorAssignment> crewAssignments);
}
