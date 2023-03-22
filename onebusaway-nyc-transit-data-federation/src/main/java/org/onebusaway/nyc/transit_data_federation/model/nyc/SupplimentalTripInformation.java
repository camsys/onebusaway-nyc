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

package org.onebusaway.nyc.transit_data_federation.model.nyc;

import java.io.Serializable;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifImport.StifTripType;

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
