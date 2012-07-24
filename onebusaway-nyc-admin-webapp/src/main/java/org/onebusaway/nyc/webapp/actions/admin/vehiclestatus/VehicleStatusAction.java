package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.VehicleGridResponse;
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
	private String rows;
	private String page;
	List<VehicleStatus> vehicleStatusRecords;
	
	public String getVehicleData() {
		List<VehicleStatus> vehiclesPerPage = null;
		Integer pageNum = new Integer(page);
		Integer rowsPerPage = new Integer(rows);
		
		//Load new records only when page number is 1 (refresh grid) event. For all other pages
		//paginate the records that we already have
		if(pageNum.equals(1)) {
			vehicleStatusRecords = vehicleStatusService.getVehicleStatus(true);
		} else {
			vehicleStatusRecords = vehicleStatusService.getVehicleStatus(false);
		}
		vehiclesPerPage = getVehiclesPerPage(vehicleStatusRecords, rowsPerPage, pageNum);
		vehicleGridResponse = new VehicleGridResponse();
		//Set page number
		vehicleGridResponse.setPage(page);
		vehicleGridResponse.setRecords(String.valueOf(rowsPerPage * pageNum));
		vehicleGridResponse.setRows(vehiclesPerPage);
		//Set total pages (no of records / records per page)
		String totalPages = new BigDecimal(vehicleStatusRecords.size())
								.divide(new BigDecimal(rows), BigDecimal.ROUND_HALF_UP).toPlainString();
		vehicleGridResponse.setTotal(totalPages);
		return "vehicles";
	}
	
	private List<VehicleStatus> getVehiclesPerPage(List<VehicleStatus> vehicleStatusRecords,
			Integer rowsPerPage, Integer pageNum) {
		List<VehicleStatus> vehiclesPerPage = new ArrayList<VehicleStatus>();
		int startIndex = rowsPerPage * (pageNum - 1);
		int endIndex = (rowsPerPage * pageNum);
		vehiclesPerPage = vehicleStatusRecords.subList(startIndex, endIndex);
		return vehiclesPerPage;
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

	/**
	 * @return the rows
	 */
	public String getRows() {
		return rows;
	}

	/**
	 * @param rows the rows to set
	 */
	public void setRows(String rows) {
		this.rows = rows;
	}

	/**
	 * @return the page
	 */
	public String getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public void setPage(String page) {
		this.page = page;
	}

	/**
	 * @param vehicleStatusRecords the vehicleStatusRecords to set
	 */
	public void setVehicleStatusRecords(List<VehicleStatus> vehicleStatusRecords) {
		this.vehicleStatusRecords = vehicleStatusRecords;
	}

	/**
	 * @return the vehicleStatusRecords
	 */
	public List<VehicleStatus> getVehicleStatusRecords() {
		return vehicleStatusRecords;
	}

}
