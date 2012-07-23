package org.onebusaway.nyc.admin.service;

import java.util.List;

import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Builds vehicle status data by querying TDM and report archiver. Makes web service calls to the
 * exposed APIs on these servers to fetch the required vehicle status data.
 * @author abelsare
 *
 */
public interface VehicleStatusService {
	
	/**
	 * Creates vehicle status data by making web service calls to TDM and report archive servers
	 * @return
	 */
	List<VehicleStatus> getVehicleStatus();

}
