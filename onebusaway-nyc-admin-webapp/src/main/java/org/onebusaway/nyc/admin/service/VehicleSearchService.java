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
}
