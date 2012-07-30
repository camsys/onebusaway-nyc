package org.onebusaway.nyc.admin.service;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.util.VehicleSearchParameters;

/**
 * Performs search operation on vehicle status records for the given parameters
 * @author abelsare
 *
 */
public interface VehicleSearchService {

	/**
	 * Performs search operation on given vehicle status records. Returns records that match the 
	 * search criteria with the given search parameters. Returns empty list if none of the records
	 * match the criteria
	 * @param vehicleStatusRecords the records to be searched
	 * @param searchParameters optional parameters
	 * @return records matching the paramters
	 */
	List<VehicleStatus> search(List<VehicleStatus> vehicleStatusRecords, 
			Map<VehicleSearchParameters, String> searchParameters);
	
	/**
	 * Searches vehicles reporting emergency from the given collection of the vehicles
	 * @param vehicleStatusRecords all vehicle records available at this point
	 * @return vehicles reporting emergency status
	 */
	List<VehicleStatus> searchVehiclesInEmergency(List<VehicleStatus> vehicleStatusRecords);
	
	/**
	 * Searches vehicles inferred in revenue service i.e buses whose inferred state is either
	 * IN PROGRESS or LAYOVER_*
	 * @param vehicleStatusRecords all vehicle records available at this point
	 * @return vehicles inferred in revenue service
	 */
	List<VehicleStatus> searchVehiclesInRevenueService(List<VehicleStatus> vehicleStatusRecords);
	
	/**
	 * Searches vehicles tracked in given time. The time can be specified by the caller
	 * @param minutes time period for results should be returned
	 * @return vehicles tracked in given time
	 */
	List<VehicleStatus> searchVehiclesTracked(int minutes, List<VehicleStatus> vehicleStatusRecords);
	
	/**
	 * Searches run/blocks scheduled to be active
	 * @return run/blocks scheduled to be active
	 */
	List<VehicleStatus> searchActiveRuns();
}
