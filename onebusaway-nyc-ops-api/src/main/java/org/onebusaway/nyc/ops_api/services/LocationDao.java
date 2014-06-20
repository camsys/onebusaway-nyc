package org.onebusaway.nyc.ops_api.services;

import java.util.Date;

/**
 * Provides common interface methods for location daos
 * @author abelsare
 *
 */
public interface LocationDao {
	
	/**
	 * Creates an invalid record in the database with the given exception
	 * @param content raw message of the record that is in error
	 * @param error exception that needs to be stored
	 * @param timeReceived the time at which exception occurred
	 */
	public void handleException(String content, Throwable error, Date timeReceived);

}
