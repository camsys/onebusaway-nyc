package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class TCIPVehicleAssignmentsOutputConverter implements
    VehicleAssignmentsOutputConverter {
  
	private DepotIdTranslator depotIdTranslator = null;
	private MtaUtsToTcipVehicleAssignmentConverter dataConverter;

	private List<MtaUtsVehiclePullInPullOut> vehicleAssignInputData = null;


	public List<VehiclePullInOutInfo> convertAssignments() {

		dataConverter.setDepotIdTranslator(depotIdTranslator);

		List<VehiclePullInOutInfo> vehAssigns = new ArrayList<VehiclePullInOutInfo>();


		for(MtaUtsVehiclePullInPullOut input : vehicleAssignInputData) {
			VehiclePullInOutInfo vehiclePullInOutInfo = new VehiclePullInOutInfo();
			SCHPullInOutInfo schPullOutInfo = dataConverter.convertToPullOut(input);
			vehiclePullInOutInfo.setPullOutInfo(schPullOutInfo);
			SCHPullInOutInfo schPullInInfo = dataConverter.convertToPullIn(input);
			vehiclePullInOutInfo.setPullInInfo(schPullInInfo);
			vehAssigns.add(vehiclePullInOutInfo);
		}

		return vehAssigns;
	}

	public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
		this.depotIdTranslator = depotIdTranslator;    
	}

	/**
	 * Injects data converter
	 * @param dataConverter the dataConverter to set
	 */
	@Autowired
	public void setDataConverter(
			MtaUtsToTcipVehicleAssignmentConverter dataConverter) {
		this.dataConverter = dataConverter;
	}

	public void setVehicleAssignInputData(
			List<MtaUtsVehiclePullInPullOut> vehicleAssignInputData) {
		this.vehicleAssignInputData = vehicleAssignInputData;
		
	}

}	
