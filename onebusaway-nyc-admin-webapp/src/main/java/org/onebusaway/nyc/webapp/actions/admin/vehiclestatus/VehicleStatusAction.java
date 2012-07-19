package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import java.util.List;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
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
	@Result(name="vehicles", type="json", params= {"root","vehicleStatusRecords"})
}
)
public class VehicleStatusAction extends OneBusAwayNYCAdminActionSupport {
	
	private static final long serialVersionUID = 1L;
	
	private List<VehicleStatus> vehicleStatusRecords;
	private VehicleStatusService vehicleStatusService;
	
	public String getVehicleData() {
		vehicleStatusRecords = vehicleStatusService.getVehicleStatus();
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
	 * Returns a list of vehicle status info
	 * @return the vehicleStatus
	 */
	public List<VehicleStatus> getVehicleStatusRecords() {
		return vehicleStatusRecords;
	}

}
