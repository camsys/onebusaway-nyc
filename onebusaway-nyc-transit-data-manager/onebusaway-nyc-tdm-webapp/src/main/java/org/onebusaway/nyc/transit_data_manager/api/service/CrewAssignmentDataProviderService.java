package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.CrewResource;

/**
 * Reads and parses data from the input files and provides data to {@link CrewResource}. 
 * It checks the most recent input file received and generates objects required by the resources.
 * @author abelsare
 *
 */
public interface CrewAssignmentDataProviderService {

	/**
	 * Processes most recent input file with pullout information and builds {@link OperatorAssignmentData}
	 * object with all crew assignment records.
	 * @param depotIdTranslator for depot id translation
	 * @return pullout data in the form required by API resources
	 */
	OperatorAssignmentData getCrewAssignmentData(DepotIdTranslator depotIdTranslator);
}
