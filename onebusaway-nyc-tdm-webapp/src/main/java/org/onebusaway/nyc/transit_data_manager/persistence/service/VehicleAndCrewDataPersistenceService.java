/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
