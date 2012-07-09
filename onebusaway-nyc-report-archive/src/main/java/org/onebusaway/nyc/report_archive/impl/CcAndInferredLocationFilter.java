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
	
	/** Holds bounding box URL parameter. Does not match any property **/
	BOUNDING_BOX("bBox");
	
	private String field;
	
	private CcAndInferredLocationFilter(String value) {
		this.field = value;
	}
	
	public String getValue() {
		return field;
	}
	
}
