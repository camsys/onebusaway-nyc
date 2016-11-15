package org.onebusaway.nyc.transit_data_federation.model.nyc;

import java.io.Serializable;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripType;

/**
 * Additional non-customer information for a trip. 
 * Includes Direction, Trip Type, and Bus Type 
 * @author laidig
 *
 */
public class SupplimentalTripInformation implements Serializable {


	private static final long serialVersionUID = 1L;
	private StifTripType tripType;
	private char busType;
	private String direction;
	
	public SupplimentalTripInformation(StifTripType tripType, char busType, String direction) {
		super();
		this.tripType = tripType;
		this.busType = busType;
		this.direction = direction;
	}

	public StifTripType getTripType() {
		return tripType;
	}
	public void setTripType(StifTripType tripType) {
		this.tripType = tripType;
	}
	public char getBusType() {
		return busType;
	}
	public void setBusType(char busType) {
		this.busType = busType;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	
	@Override
	public String toString() {
		return "SupplimentalTripInformation [tripType=" + tripType + ", busType=" + busType + ", direction=" + direction
				+ "]";
	}
}
