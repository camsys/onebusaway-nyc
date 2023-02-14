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

package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

/**
 * A model object representing a vehicle (most likely a bus!) For use with Gson
 * to make Json.
 * 
 * @author sclark
 * 
 */
public class Vehicle {
	public Vehicle() {

	}

	private String agencyId;
	private String vehicleId;
	private String depotId;
	private Boolean kneeling;

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public void setDepotId(String depot) {
		this.depotId = depot;
	}
	
	public String getVehicleId() {
		return vehicleId;
	}

	public void setKneeling(Boolean kneeling) {
		this.kneeling = kneeling;
	}

	/**
	 * @return the agencyId
	 */
	public String getAgencyId() {
		return agencyId;
	}

	/**
	 * @return the depotId
	 */
	public String getDepotId() {
		return depotId;
	}

	/**
	 *
	 * @return if the vehicle is a kneeling buss
	 */
	public Boolean isKneeling() {
		return kneeling;
	}
}
