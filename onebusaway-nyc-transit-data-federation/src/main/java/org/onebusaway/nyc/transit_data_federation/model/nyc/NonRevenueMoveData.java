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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripType;

import com.vividsolutions.jts.geom.Coordinate;

public class NonRevenueMoveData implements Serializable {

	private static final long serialVersionUID = 1L;

	private AgencyAndId runId;

	private StifTripType moveType;

	private String depotCode;

	// seconds past midnight
	private int startTime;

	// seconds past midnight
	private int endTime;

	private Coordinate startLocation;

	private Coordinate endLocation;

	public String toString() {
		return "NonRevenueMove(" + moveType + ", " + startTime + ", " + endTime + ")";
	}
	
	public String getDepotCode() {
		return depotCode;
	}

	public void setDepotCode(String depotCode) {
		this.depotCode = depotCode;
	}

	public AgencyAndId getRunId() {
		return runId;
	}

	public void setRunId(AgencyAndId runId) {
		this.runId = runId;
	}

	public StifTripType getMoveType() {
		return moveType;
	}

	public void setMoveType(StifTripType moveType) {
		this.moveType = moveType;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	public Coordinate getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Coordinate startLocation) {
		this.startLocation = startLocation;
	}

	public Coordinate getEndLocation() {
		return endLocation;
	}

	public void setEndLocation(Coordinate endLocation) {
		this.endLocation = endLocation;
	}

}
