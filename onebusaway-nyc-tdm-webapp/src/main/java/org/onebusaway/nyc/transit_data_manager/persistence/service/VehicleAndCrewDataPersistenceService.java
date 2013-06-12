package org.onebusaway.nyc.transit_data_manager.persistence.service;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Persists crew and vehicle pullout data. This serves as an entry point for the scheduler if data
 * persistence needs to be scheduled at regular intervals.
 * @author abelsare
 *
 */
public interface VehicleAndCrewDataPersistenceService {
	
	/**
	 * Persists vehicle pullout data from the final file per service date.
	 */
	void saveVehiclePulloutData() throws DataAccessResourceFailureException;
	
	/**
	 * Persists crew assignment data from the final file per service date.
	 */
	void saveCrewAssignmentData() throws DataAccessResourceFailureException;

}
