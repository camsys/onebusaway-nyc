package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;

import tcip_final_4_0_0.CPTVehicleIden;

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
