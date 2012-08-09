package org.onebusaway.nyc.report_archive.impl;

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
	VEHICLE_AGENCY_ID("vehicleAgencyId"),
	
	/** Holds bounding box URL parameter. Does not match any property **/
	BOUNDING_BOX("bBox"),
	
	/** Start date parameter for time bound query **/
	START_DATE("start-date"),
	
	/** End date parameter for time bound query **/
	END_DATE("end-date"),
	
	/** Number of records to be returned by the query **/
	RECORDS("records");
	
	private String field;
	
	private CcAndInferredLocationFilter(String value) {
		this.field = value;
	}
	
	public String getValue() {
		return field;
	}
	
}
