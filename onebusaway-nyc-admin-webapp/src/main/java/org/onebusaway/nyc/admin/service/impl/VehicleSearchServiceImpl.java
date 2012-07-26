package org.onebusaway.nyc.admin.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;
import org.onebusaway.nyc.admin.search.impl.DSCFilter;
import org.onebusaway.nyc.admin.search.impl.DepotFilter;
import org.onebusaway.nyc.admin.search.impl.EmergencyStatusFilter;
import org.onebusaway.nyc.admin.search.impl.InferredStateFilter;
import org.onebusaway.nyc.admin.search.impl.RouteFilter;
import org.onebusaway.nyc.admin.search.impl.VehicleIdFilter;
import org.onebusaway.nyc.admin.service.VehicleSearchService;
import org.onebusaway.nyc.admin.util.VehicleSearchParameters;

/**
 * Default implementation of {@link VehicleSearchService}
 * @author abelsare
 *
 */
public class VehicleSearchServiceImpl implements VehicleSearchService {

	@Override
	public List<VehicleStatus> search(List<VehicleStatus> vehicleStatusRecords,
			Map<VehicleSearchParameters, String> searchParameters) {
		
		List<VehicleStatus> matchingRecords = new ArrayList<VehicleStatus>();

		//Build filters corresponding to the search parameters
		List<Filter<VehicleStatus>> filters = buildFilters(searchParameters);
		
		//Since there are no filters specified, return all the records as matched records
		if(filters.isEmpty()) {
			matchingRecords.addAll(vehicleStatusRecords);
		} else {
			//Apply each filter to each record
			for(VehicleStatus vehicleStatus : vehicleStatusRecords) {
				boolean matches = applyFilters(vehicleStatus, filters);
				if(matches) {
					matchingRecords.add(vehicleStatus);
				}
			}
		}
		
		return matchingRecords;
	}
	
	private boolean applyFilters(VehicleStatus vehicleStatus, List<Filter<VehicleStatus>> filters) {
		boolean match = false;
		for(Filter<VehicleStatus> filter : filters) {
			if(filter.apply(vehicleStatus)) {
				match = true;
			} else {
				//Break on first non match
				match = false;
				break;
			}
		}
		return match;
	}
	
	private List<Filter<VehicleStatus>> buildFilters(
			Map<VehicleSearchParameters, String> searchParameters) {
		List<Filter<VehicleStatus>> filters = new ArrayList<Filter<VehicleStatus>>();
		
		//Since all parameters are optional we have to look for each one
		//To-do: there might be a better way of doing this
		String vehicleId = searchParameters.get(VehicleSearchParameters.VEHICLE_ID);
		if(StringUtils.isNotBlank(vehicleId)) {
			filters.add(new VehicleIdFilter(vehicleId));
		}
		String route = searchParameters.get(VehicleSearchParameters.ROUTE);
		if(StringUtils.isNotBlank(route)) {
			filters.add(new RouteFilter(route));
		}
		String inferredState = searchParameters.get(VehicleSearchParameters.INFERRED_STATE);
		if(StringUtils.isNotBlank(inferredState) && !inferredState.equalsIgnoreCase("All")) {
			filters.add(new InferredStateFilter(inferredState));
		}
		String dsc = searchParameters.get(VehicleSearchParameters.DSC);
		if(StringUtils.isNotBlank(dsc)) {
			filters.add(new DSCFilter(dsc));
		}
		String depot = searchParameters.get(VehicleSearchParameters.DEPOT);
		if(StringUtils.isNotBlank(depot) && !depot.equalsIgnoreCase("All")) {
			filters.add(new DepotFilter(depot));
		}
		String emergencyStatus = searchParameters.get(VehicleSearchParameters.EMERGENCY_STATUS);
		if(emergencyStatus.equalsIgnoreCase("true")) {
			filters.add(new EmergencyStatusFilter());
		}
		return filters;
	}

}
