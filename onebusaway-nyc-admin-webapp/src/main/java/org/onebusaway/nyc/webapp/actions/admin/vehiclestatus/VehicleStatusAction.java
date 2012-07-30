package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.xwork.StringUtils;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.VehicleGridResponse;
import org.onebusaway.nyc.admin.model.ui.VehicleStatistics;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.service.VehicleStatusService;
import org.onebusaway.nyc.admin.util.VehicleSearchParameters;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Action class for vehicle status UI
 * @author abelsare
 *
 */
@Namespace(value="/admin/vehiclestatus")
@Results({
	@Result(name="vehicles", type="json", params= {"root","vehicleGridResponse"}),
	@Result(name="statistics", type="json", params= {"root", "vehicleStatistics"})
}
)
public class VehicleStatusAction extends OneBusAwayNYCAdminActionSupport {
	
	private static final long serialVersionUID = 1L;
	
	private VehicleStatusService vehicleStatusService;
	private VehicleGridResponse vehicleGridResponse;
	private VehicleStatistics vehicleStatistics;
	//Request URL parameters
	private String rows;
	private String page;
	private boolean _search;
	private String vehicleId;
	private String route;
	private String depot;
	private String dsc;
	private String inferredState;
	private String pulloutStatus;
	private String emergencyStatus;
	
	
	public String getVehicleData() {
		List<VehicleStatus> vehiclesPerPage = null;
		List<VehicleStatus> vehicleStatusRecords = null;
		Integer pageNum = new Integer(page);
		Integer rowsPerPage = new Integer(rows);
		int total = 0;
		
		//Load new records only when page number is 1 (refresh grid) event. For all other pages
		//fetch the records that we already have from the cache
		if(pageNum.equals(1) && _search == false) {
			vehicleStatusRecords = vehicleStatusService.getVehicleStatus(true);
			//Return all the records as this is not search request
			vehiclesPerPage = getVehiclesPerPage(vehicleStatusRecords, rowsPerPage, pageNum);
			total = vehicleStatusRecords.size();
		} else {
			vehicleStatusRecords = vehicleStatusService.getVehicleStatus(false);
			if(_search) {
				//This is a search request.Perform search operation and return matching records.
				Map<VehicleSearchParameters, String> searchParameters = buildSearchParameters();
				List<VehicleStatus> matchingVehicleRecords = null;
				//Perform new search if page number is 1, load the results from cache otherwise
				if(pageNum.equals(1)) {
					matchingVehicleRecords = vehicleStatusService.search(searchParameters, true); 
				} else {
					matchingVehicleRecords = vehicleStatusService.search(searchParameters, false);
				}
				vehiclesPerPage = getVehiclesPerPage(matchingVehicleRecords, rowsPerPage, pageNum);
				total = matchingVehicleRecords.size();
			} else {
				//Subsequent pages for non search requests
				vehiclesPerPage = getVehiclesPerPage(vehicleStatusRecords, rowsPerPage, pageNum);
				total = vehicleStatusRecords.size();
			}
		}
		
		buildResponse(vehiclesPerPage, pageNum, rowsPerPage, total);
		
		return "vehicles";
	}
	
	public String getStatistics() {
		vehicleStatistics = vehicleStatusService.getVehicleStatistics();
		return "statistics";
	}

	private void buildResponse(List<VehicleStatus> vehicleRecordsPerPage,
			Integer pageNum, Integer rowsPerPage, int totalRecords) {
		String totalPages = StringUtils.EMPTY;
		vehicleGridResponse = new VehicleGridResponse();
		//Set page number
		vehicleGridResponse.setPage(page);
		//Calculate records per page
		if(totalRecords <= (rowsPerPage * pageNum)) {
			totalPages = String.valueOf(pageNum);
		} else {
			//Set total pages (no of records / records per page)
			totalPages = new BigDecimal(totalRecords).divide(
					new BigDecimal(rowsPerPage), BigDecimal.ROUND_UP).toPlainString();
		}
		vehicleGridResponse.setRecords(String.valueOf(totalRecords));
		vehicleGridResponse.setRows(vehicleRecordsPerPage);
		
		vehicleGridResponse.setTotal(totalPages);
	}
	
	private List<VehicleStatus> getVehiclesPerPage(List<VehicleStatus> vehicleStatusRecords,
			Integer rowsPerPage, Integer pageNum) {
		List<VehicleStatus> vehiclesPerPage = new ArrayList<VehicleStatus>();
		int startIndex = rowsPerPage * (pageNum - 1);
		int endIndex = (rowsPerPage * pageNum);
		if(endIndex > vehicleStatusRecords.size()) {
			endIndex = vehicleStatusRecords.size();
		}
		vehiclesPerPage = vehicleStatusRecords.subList(startIndex, endIndex);
		return vehiclesPerPage;
	}
	
	private Map<VehicleSearchParameters, String> buildSearchParameters() {
		Map<VehicleSearchParameters, String> searchParameters = new HashMap<VehicleSearchParameters, String>();
		searchParameters.put(VehicleSearchParameters.VEHICLE_ID, vehicleId);
		searchParameters.put(VehicleSearchParameters.ROUTE, route);
		searchParameters.put(VehicleSearchParameters.DEPOT, depot);
		searchParameters.put(VehicleSearchParameters.DSC, dsc);
		searchParameters.put(VehicleSearchParameters.INFERRED_STATE, inferredState);
		searchParameters.put(VehicleSearchParameters.PULLOUT_STATUS, pulloutStatus);
		searchParameters.put(VehicleSearchParameters.EMERGENCY_STATUS, emergencyStatus);
		
		return searchParameters;
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
	 * @return the _search
	 */
	public boolean is_search() {
		return _search;
	}

	/**
	 * @param _search the _search to set
	 */
	public void set_search(boolean _search) {
		this._search = _search;
	}

	/**
	 * @return the vehicleId
	 */
	public String getVehicleId() {
		return vehicleId;
	}

	/**
	 * @param vehicleId the vehicleId to set
	 */
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	/**
	 * @return the route
	 */
	public String getRoute() {
		return route;
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(String route) {
		this.route = route;
	}

	/**
	 * @return the depot
	 */
	public String getDepot() {
		return depot;
	}

	/**
	 * @param depot the depot to set
	 */
	public void setDepot(String depot) {
		this.depot = depot;
	}

	/**
	 * @return the dsc
	 */
	public String getDsc() {
		return dsc;
	}

	/**
	 * @param dsc the dsc to set
	 */
	public void setDsc(String dsc) {
		this.dsc = dsc;
	}

	/**
	 * @return the inferredState
	 */
	public String getInferredState() {
		return inferredState;
	}

	/**
	 * @param inferredState the inferredState to set
	 */
	public void setInferredState(String inferredState) {
		this.inferredState = inferredState;
	}

	/**
	 * @return the pulloutStatus
	 */
	public String getPulloutStatus() {
		return pulloutStatus;
	}

	/**
	 * @param pulloutStatus the pulloutStatus to set
	 */
	public void setPulloutStatus(String pulloutStatus) {
		this.pulloutStatus = pulloutStatus;
	}

	/**
	 * @return the emergencyStatus
	 */
	public String getEmergencyStatus() {
		return emergencyStatus;
	}

	/**
	 * @param emergencyStatus the emergencyStatus to set
	 */
	public void setEmergencyStatus(String emergencyStatus) {
		this.emergencyStatus = emergencyStatus;
	}

	/**
	 * @return the vehicleStatistics
	 */
	public VehicleStatistics getVehicleStatistics() {
		return vehicleStatistics;
	}

}
