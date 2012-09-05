package org.onebusaway.nyc.transit_data_manager.persistence.service;

/**
 * Persists crew and vehicle pullout data. This serves as an entry point for the scheduler if the data
 * persistence needs to be scheduled at regular intervals.
 * @author abelsare
 *
 */
public interface UTSDataPersistenceService {
	
	/**
	 * Persists vehicle pullout data from the final file per service date.
	 */
	void saveVehiclePulloutData();
	
	/**
	 * Persists crew assignment data from the final file per service date.
	 */
	void saveCrewAssignmentData();

}
