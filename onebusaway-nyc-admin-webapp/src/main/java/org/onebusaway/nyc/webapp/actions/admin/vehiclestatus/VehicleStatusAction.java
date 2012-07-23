package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.ui.VehicleGridResponse;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.service.VehicleStatusService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Action class for vehicle status UI
 * @author abelsare
 *
 */
@Namespace(value="/admin/vehiclestatus")
@Results({
	@Result(name="vehicles", type="json", params= {"root","vehicleGridResponse"})
}
)
public class VehicleStatusAction extends OneBusAwayNYCAdminActionSupport {
	
	private static final long serialVersionUID = 1L;
	
	private VehicleStatusService vehicleStatusService;
	private VehicleGridResponse vehicleGridResponse;
	
	public String getVehicleData() {
		List<VehicleStatus> vehicleStatusRecords = vehicleStatusService.getVehicleStatus();
		vehicleGridResponse = new VehicleGridResponse();
		//Create 100 pages for now. Need to come up with a realistic number based on the data
		vehicleGridResponse.setPage("1");
		vehicleGridResponse.setRecords(String.valueOf(vehicleStatusRecords.size()));
		vehicleGridResponse.setRows(vehicleStatusRecords);
		vehicleGridResponse.setTotal("100");
		return "vehicles";
	}

	/**
	 * Injects vehicle status service
	 * @param vehicleStatusService the vehicleStatusService to set
	 */
	@Autowired
	public void setVehicleStatusService(VehicleStatusService vehicleStatusService) {
		this.vehicleStatusService = vehicleStatusService;
	}
	
	/**
	 * @return the vehicleGridResponse
	 */
	public VehicleGridResponse getVehicleGridResponse() {
		return vehicleGridResponse;
	}

}
