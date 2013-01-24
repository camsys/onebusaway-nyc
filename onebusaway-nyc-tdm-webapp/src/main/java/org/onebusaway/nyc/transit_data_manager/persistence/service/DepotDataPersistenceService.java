package org.onebusaway.nyc.transit_data_manager.persistence.service;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Persists depot data. This serves as an entry point for the scheduler 
 * if data persistence needs to be scheduled at regular intervals.
 * @author abelsare
 *
 */
public interface DepotDataPersistenceService {
	
	/**
	 * Persists depot data from the final file per service date.
	 */
	void saveDepotData() throws DataAccessResourceFailureException;

}
