package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;


public interface VehicleAssignmentsOutputConverter {
  
	/**
	 * Converts parsed input data in the required format.
	 * @return list of pull in pull out data in the required format.
	 */
	List<VehiclePullInOutInfo> convertAssignments();

	/**
	 * Injects {@link DepotIdTranslator}
	 * @param depotIdTranslator 
	 */
	void setDepotIdTranslator(DepotIdTranslator depotIdTranslator);

	/**
	 * Injects data to convert
	 * @param vehicleAssignInputData vehicle input data to convert
	 */
	void setVehicleAssignInputData(List<MtaUtsVehiclePullInPullOut> vehicleAssignInputData);
}
