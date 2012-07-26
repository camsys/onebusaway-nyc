package org.onebusaway.nyc.admin.util;

/**
 * Holds parameters used for searching vehicles
 * @author abelsare
 *
 */
public enum VehicleSearchParameters {
	
	VEHICLE_ID("vehicleId"),
	ROUTE("route"),
	DEPOT("depot"),
	DSC("dsc"),
	INFERRED_STATE("inferredState"),
	PULLOUT_STATUS("pulloutStatus"),
	EMERGENCY_STATUS("emergencyStatus");
	
	private String value;

	private VehicleSearchParameters(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}
