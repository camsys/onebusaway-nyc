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

package org.onebusaway.nyc.report.impl;

/**
 * Holds request URL paramter constants
 * @author abelsare
 *
 */
public enum CcAndInferredLocationFilter {

	/** Matches CcAndInferredLocationRecord.depotId field**/
	DEPOT_ID("depotId"),
	
	/** Matches CcAndInferredLocationRecord.inferredRouteId field**/
	INFERRED_ROUTEID("inferredRouteId"),
	
	/** Matches CcAndInferredLocationRecord.inferredPhase field **/
	INFERRED_PHASE("inferredPhase"),
	
	/** Matches CcLocationReportRecord.vehicleId field **/
	VEHICLE_ID("vehicleId"),
	
	/** Matches CcLocationReportRecord.vehicleAgencyId field **/
	VEHICLE_AGENCY_ID("vehicleAgencyDesignator"),
	
	/** Holds bounding box URL parameter. Does not match any property **/
	BOUNDING_BOX("bBox"),
	
	/** Start date parameter for time bound query **/
	START_DATE("start-date"),
	
	/** End date parameter for time bound query **/
	END_DATE("end-date"),
	
	/** Number of records to be returned by the query **/
	RECORDS("records"), 
	/** How long to wait for a response **/
	TIMEOUT("timeout");
	
	private String field;
	
	private CcAndInferredLocationFilter(String value) {
		this.field = value;
	}
	
	public String getValue() {
		return field;
	}
	
}
