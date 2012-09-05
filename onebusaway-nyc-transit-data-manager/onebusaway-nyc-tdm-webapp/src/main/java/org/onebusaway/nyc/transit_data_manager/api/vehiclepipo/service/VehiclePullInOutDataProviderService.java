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
	 * Processes most recent input file with pullout information and builds {@link ImporterVehiclePulloutData}
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
