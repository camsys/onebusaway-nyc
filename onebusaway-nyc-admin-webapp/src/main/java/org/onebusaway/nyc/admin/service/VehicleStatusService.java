package org.onebusaway.nyc.admin.service;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.admin.model.ui.VehicleDetail;
import org.onebusaway.nyc.admin.model.ui.VehicleStatistics;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.util.VehicleSearchParameters;

/**
 * Builds vehicle status data by querying TDM and report archiver. Makes web service calls to the
 * exposed APIs on these servers to fetch the required vehicle status data.
 * @author abelsare
 *
 */
public interface VehicleStatusService {
	
	/**
	 * Creates vehicle status data by making web service calls to TDM and report archive servers
	 * @param indicates whether new data should be loaded. The service returns cached data otherwise
	 * @return new/cached vehicle status data
	 */
	List<VehicleStatus> getVehicleStatus(boolean loadNew);
	
	/**
	 * Searches vehicles based on the given parameters. Uses vehicle cache for fetching vehicle data.
	 * Returns empty results if cache is empty
	 * @param searchParameters paramters for searching
	 * @param newSearch indicates this is a new search
	 * @return results matching the parameters, empty list if the cache is empty
	 */
	List<VehicleStatus> search(Map<VehicleSearchParameters, String> searchParameters, boolean newSearch);
	
	/**
	 * Returns statistics of the vehicles tracked such as vehicles reporting emergency, vehicles in
	 * revenue servie, vehicles tracked in past five minutes
	 * @param parameters optional parameters from client 
	 * @return vehicle statistics with the required count
	 */
	VehicleStatistics getVehicleStatistics(String... parameters);

	/**
	 * Make a web service call to report archive server about a specific vehicle.
	 * @param vehicleId
	 * @return
	 */
  VehicleDetail getVehicleDetail(String vehicleId);

}
