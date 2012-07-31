package org.onebusaway.nyc.admin.util;

/**
 * Holds fields on which vehicle records can be sorted
 * @author abelsare
 *
 */
public enum VehicleSortFields {
	
	VEHICLEID("vehicleId"),
	LASTUPDATE("lastUpdate"),
	INFERREDSTATE("inferredState"),
	OBSERVEDDSC("observedDSC"),
	PULLOUTTIME("pulloutTime"),
	PULLINTIME("pullinTime");
	
	private String field;
	
	private VehicleSortFields(String field) {
		this.field = field;
	}
	
	public String getField() {
		return field;
	}

}
