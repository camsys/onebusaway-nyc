package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;


public interface PulloutData {
	
	/**
	 * Returns all pull out data for all buses
	 * @return pull out data
	 */
	List<VehiclePullInOutInfo> getAllPullouts();
	
	/**
	 * Returns pull out data for the given bus number
	 * @param busNumber bus number for which pull out data is desired
	 * @return pull out data for the given bus number
	 */
	List<VehiclePullInOutInfo> getPulloutsByBus(Long busNumber);
	
	/**
	 * Returns pull out data for the given depot
	 * @param depotId depot for which pull data is desired
	 * @return pull out data for given depot
	 */
	List<VehiclePullInOutInfo> getPulloutsByDepot(String depotId);
}
